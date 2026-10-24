package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.MatriculaRequest;
import ifsp.edu.sistema_academia.dto.request.TrocaPlanoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoMatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.MatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.TrocaPlanoResponse;
import ifsp.edu.sistema_academia.exception.MatriculaAtivaExistenteException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.MatriculaRepository;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * RF04 (matricular), RF06 (trocar de plano) e RF07 (cancelar matrícula). Cada operação que altera várias linhas
 * (matrícula + mensalidades) roda em uma única transação: ou tudo é gravado, ou nada é. As operações sobre um
 * aluno são serializadas pelo lock da linha do aluno.
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanoRepository planoRepository;
    private final Clock clock;

    public MatriculaService(MatriculaRepository matriculaRepository, UsuarioRepository usuarioRepository,
                            PlanoRepository planoRepository, Clock clock) {
        this.matriculaRepository = matriculaRepository;
        this.usuarioRepository = usuarioRepository;
        this.planoRepository = planoRepository;
        this.clock = clock;
    }

    @Transactional
    public MatriculaResponse matricular(MatriculaRequest request) {
        LocalDate hoje = LocalDate.now(clock);
        Usuario aluno = bloquearAluno(request.alunoId());
        Plano plano = buscarPlano(request.planoId());
        if (matriculaRepository.findAtiva(aluno.getId(), hoje).isPresent()) {
            throw new MatriculaAtivaExistenteException(aluno.getId());
        }
        Matricula matricula = matriculaRepository.save(Matricula.iniciar(aluno, plano, hoje, BigDecimal.ZERO));
        return MatriculaResponse.from(matricula, hoje);
    }

    /**
     * Encerra a matrícula atual, cancela as mensalidades futuras e cria a nova matrícula. O valor proporcional aos
     * dias não usados do mês já pago vira desconto na primeira mensalidade da nova matrícula.
     */
    @Transactional
    public TrocaPlanoResponse trocarPlano(Long matriculaId, TrocaPlanoRequest request) {
        LocalDate hoje = LocalDate.now(clock);
        Matricula atual = carregarParaAlteracao(matriculaId);
        exigirAtiva(atual, hoje);
        Plano novoPlano = buscarPlano(request.novoPlanoId());
        if (novoPlano.getId().equals(atual.getPlano().getId())) {
            throw new RegraNegocioException("O novo plano deve ser diferente do plano atual");
        }

        BigDecimal credito = atual.creditoNaoUtilizado(hoje);
        List<Mensalidade> canceladas = atual.encerrarPorTroca(hoje);
        Matricula nova = matriculaRepository.save(Matricula.iniciar(atual.getAluno(), novoPlano, hoje, credito));
        BigDecimal descontoAplicado = nova.getMensalidades().get(0).getDesconto();

        return new TrocaPlanoResponse(MatriculaResponse.from(atual, hoje), MatriculaResponse.from(nova, hoje),
                credito, descontoAplicado, canceladas.size());
    }

    /** Cancela a matrícula e as mensalidades futuras; planos de 3 ou 12 meses pagam multa de 10% sobre elas. */
    @Transactional
    public CancelamentoMatriculaResponse cancelar(Long matriculaId) {
        LocalDate hoje = LocalDate.now(clock);
        Matricula matricula = carregarParaAlteracao(matriculaId);
        exigirAtiva(matricula, hoje);

        List<Mensalidade> canceladas = matricula.cancelar(hoje);
        return new CancelamentoMatriculaResponse(MatriculaResponse.from(matricula, hoje), canceladas.size(),
                Matricula.valorTotal(canceladas), matricula.getMultaCancelamento());
    }

    @Transactional(readOnly = true)
    public MatriculaResponse buscar(Long id, UsuarioAutenticado solicitante) {
        Matricula matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Matrícula", id));
        solicitante.exigirAcessoAoAluno(matricula.getAluno().getId());
        return MatriculaResponse.from(matricula, LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public MatriculaResponse buscarAtiva(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        LocalDate hoje = LocalDate.now(clock);
        return matriculaRepository.findAtiva(alunoId, hoje)
                .map(m -> MatriculaResponse.from(m, hoje))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O aluno " + alunoId + " não possui matrícula ativa"));
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorAluno(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        buscarAlunoExistente(alunoId);
        LocalDate hoje = LocalDate.now(clock);
        return matriculaRepository.findByAlunoIdOrderByDataInicioDescIdDesc(alunoId).stream()
                .map(m -> MatriculaResponse.from(m, hoje))
                .toList();
    }

    /** Descobre o aluno da matrícula, bloqueia a linha dele e só então carrega a matrícula (estado atualizado). */
    private Matricula carregarParaAlteracao(Long matriculaId) {
        Long alunoId = matriculaRepository.findAlunoIdById(matriculaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Matrícula", matriculaId));
        bloquearAluno(alunoId);
        return matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Matrícula", matriculaId));
    }

    private void exigirAtiva(Matricula matricula, LocalDate hoje) {
        if (!matricula.estaAtiva(hoje)) {
            throw new MatriculaNaoAtivaException("A matrícula " + matricula.getId() + " não está ativa");
        }
    }

    private Usuario bloquearAluno(Long alunoId) {
        return usuarioRepository.findByIdAndPerfilForUpdate(alunoId, Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
    }

    private Usuario buscarAlunoExistente(Long alunoId) {
        return usuarioRepository.findByIdAndPerfil(alunoId, Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
    }

    private Plano buscarPlano(Long planoId) {
        return planoRepository.findById(planoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", planoId));
    }
}