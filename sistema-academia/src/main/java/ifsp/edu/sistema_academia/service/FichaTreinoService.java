package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.FichaTreinoRequest;
import ifsp.edu.sistema_academia.dto.response.FichaTreinoResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.FichaTreino;
import ifsp.edu.sistema_academia.model.ItemTreino;
import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.model.Instrutor;
import ifsp.edu.sistema_academia.repository.FichaTreinoRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
import ifsp.edu.sistema_academia.repository.InstrutorRepository;
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
    private final AlunoRepository alunoRepository;
    private final InstrutorRepository instrutorRepository;
    private final Clock clock;

    public FichaTreinoService(FichaTreinoRepository fichaRepository, AlunoRepository alunoRepository,
                              InstrutorRepository instrutorRepository, Clock clock) {
        this.fichaRepository = fichaRepository;
        this.alunoRepository = alunoRepository;
        this.instrutorRepository = instrutorRepository;
        this.clock = clock;
    }

    @Transactional
    public FichaTreinoResponse criar(Long alunoId, FichaTreinoRequest request, UsuarioAutenticado instrutorLogado) {
        Aluno aluno = alunoRepository.findByIdForUpdate(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        Instrutor instrutor = instrutorRepository.findById(instrutorLogado.id())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Instrutor", instrutorLogado.id()));

        // A ficha anterior vai para o histórico e a nova é gravada na mesma transação.
        fichaRepository.findAllByAlunoIdAndIsAtualTrue(alunoId).forEach(FichaTreino::arquivar);

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
        return fichaRepository.findFirstByAlunoIdAndIsAtualTrueOrderByCriadaEmDescIdDesc(alunoId)
                .map(FichaTreinoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O aluno " + alunoId + " ainda não possui ficha de treino"));
    }

    @Transactional(readOnly = true)
    public List<FichaTreinoResponse> listarHistorico(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        validarAluno(alunoId);
        return fichaRepository.findByAlunoIdAndIsAtualFalseOrderByCriadaEmDescIdDesc(alunoId).stream()
                .map(FichaTreinoResponse::from)
                .toList();
    }

    private void validarAluno(Long alunoId) {
        alunoRepository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
    }
}
