package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.FichaTreinoRequest;
import ifsp.edu.sistema_academia.dto.response.FichaTreinoResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.FichaTreino;
import ifsp.edu.sistema_academia.model.ItemTreino;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.FichaTreinoRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** RF13: o instrutor monta a ficha; a anterior vai para o histórico. O aluno vê a própria ficha atual e o histórico. */
@Service
public class FichaTreinoService {

    private final FichaTreinoRepository fichaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public FichaTreinoService(FichaTreinoRepository fichaRepository, UsuarioRepository usuarioRepository,
                              Clock clock) {
        this.fichaRepository = fichaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public FichaTreinoResponse criar(Long alunoId, FichaTreinoRequest request, UsuarioAutenticado instrutorLogado) {
        Usuario aluno = usuarioRepository.findByIdAndPerfilForUpdate(alunoId, Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        Usuario instrutor = usuarioRepository.findById(instrutorLogado.id())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Instrutor", instrutorLogado.id()));

        // A ficha anterior vai para o histórico e a nova é gravada na mesma transação.
        fichaRepository.findByAlunoIdAndAtualTrue(alunoId).ifPresent(FichaTreino::arquivar);

        List<ItemTreino> itens = request.itens().stream()
                .map(i -> new ItemTreino(i.exercicio().trim(), i.series(), i.repeticoes(),
                        i.cargaKg().setScale(2, RoundingMode.HALF_UP)))
                .toList();
        String observacoes = request.observacoes() == null || request.observacoes().isBlank()
                ? null
                : request.observacoes().trim();
        FichaTreino ficha = fichaRepository.save(
                new FichaTreino(aluno, instrutor, LocalDateTime.now(clock), observacoes, itens));
        return FichaTreinoResponse.from(ficha);
    }

    @Transactional(readOnly = true)
    public FichaTreinoResponse buscarAtual(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        validarAluno(alunoId);
        return fichaRepository.findByAlunoIdAndAtualTrue(alunoId)
                .map(FichaTreinoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O aluno " + alunoId + " ainda não possui ficha de treino"));
    }

    @Transactional(readOnly = true)
    public List<FichaTreinoResponse> listarHistorico(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        validarAluno(alunoId);
        return fichaRepository.findByAlunoIdAndAtualFalseOrderByCriadaEmDescIdDesc(alunoId).stream()
                .map(FichaTreinoResponse::from)
                .toList();
    }

    private void validarAluno(Long alunoId) {
        usuarioRepository.findByIdAndPerfil(alunoId, Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
    }
}
