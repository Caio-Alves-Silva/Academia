package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.exception.AlunoBloqueadoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import ifsp.edu.sistema_academia.repository.MatriculaRepository;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Regras compartilhadas sobre a situação de um aluno: matrícula ativa e bloqueio por inadimplência (RF05).
 * Usadas pela entrada na academia (RF12) e pelas inscrições em aulas (RF09/RF10).
 */
@Service
public class SituacaoAlunoService {

    private final MatriculaRepository matriculaRepository;
    private final MensalidadeRepository mensalidadeRepository;
    private final Clock clock;

    public SituacaoAlunoService(MatriculaRepository matriculaRepository,
                                MensalidadeRepository mensalidadeRepository, Clock clock) {
        this.matriculaRepository = matriculaRepository;
        this.mensalidadeRepository = mensalidadeRepository;
        this.clock = clock;
    }

    public Optional<Matricula> buscarMatriculaAtiva(Long alunoId) {
        return matriculaRepository.findAtiva(alunoId, LocalDate.now(clock));
    }

    /** Bloqueado = possui mensalidade pendente vencida há mais de 5 dias (de qualquer matrícula). */
    public boolean estaBloqueado(Long alunoId) {
        LocalDate limite = LocalDate.now(clock).minusDays(Mensalidade.DIAS_ATE_BLOQUEIO);
        return mensalidadeRepository.existsByMatriculaAlunoIdAndStatusAndVencimentoBefore(
                alunoId, StatusMensalidade.PENDENTE, limite);
    }

    /** Verdadeiro se o aluno tem matrícula ativa e não está bloqueado. */
    public boolean podeFrequentar(Long alunoId) {
        return buscarMatriculaAtiva(alunoId).isPresent() && !estaBloqueado(alunoId);
    }

    /**
     * Exige matrícula ativa e ausência de bloqueio.
     *
     * @return a matrícula ativa do aluno
     */
    public Matricula exigirLiberado(Long alunoId) {
        Matricula matricula = buscarMatriculaAtiva(alunoId)
                .orElseThrow(() -> MatriculaNaoAtivaException.doAluno(alunoId));
        if (estaBloqueado(alunoId)) {
            throw new AlunoBloqueadoException();
        }
        return matricula;
    }
}