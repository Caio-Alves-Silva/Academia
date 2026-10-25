package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.CheckinRequest;
import ifsp.edu.sistema_academia.dto.response.CheckinResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.model.Checkin;
import ifsp.edu.sistema_academia.model.TipoAcesso;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * RF12: a recepção registra a entrada. Aluno bloqueado (inadimplência > 5 dias) ou sem matrícula ativa tem a
 * tentativa persistida com {@code acessoLiberado = false} e o motivo do bloqueio, e o acesso é recusado (422).
 */
@Service
public class CheckinService {

    private final CheckinRepository checkinRepository;
    private final AlunoRepository alunoRepository;
    private final SituacaoAlunoService situacaoAlunoService;
    private final Clock clock;

    public CheckinService(CheckinRepository checkinRepository, AlunoRepository alunoRepository,
                          SituacaoAlunoService situacaoAlunoService, Clock clock) {
        this.checkinRepository = checkinRepository;
        this.alunoRepository = alunoRepository;
        this.situacaoAlunoService = situacaoAlunoService;
        this.clock = clock;
    }

    /** {@code noRollbackFor}: a recusa precisa ser gravada mesmo com a exceção de regra de negócio propagada. */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public CheckinResponse registrar(CheckinRequest request) {
        Aluno aluno = alunoRepository.findById(request.alunoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", request.alunoId()));
        TipoAcesso tipoAcesso = request.tipoAcesso() == null ? TipoAcesso.RECEPCAO : request.tipoAcesso();
        LocalDateTime agora = LocalDateTime.now(clock);
        try {
            situacaoAlunoService.exigirLiberado(aluno.getId());
        } catch (RegraNegocioException recusa) {
            checkinRepository.save(new Checkin(aluno, agora, tipoAcesso, false, recusa.getMessage()));
            throw recusa;
        }
        Checkin checkin = checkinRepository.save(new Checkin(aluno, agora, tipoAcesso, true, null));
        return CheckinResponse.from(checkin);
    }
}