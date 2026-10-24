package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.CheckinRequest;
import ifsp.edu.sistema_academia.dto.response.CheckinResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Checkin;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** RF12: a recepção registra a entrada; aluno bloqueado ou sem matrícula ativa é recusado. */
@Service
public class CheckinService {

    private final CheckinRepository checkinRepository;
    private final UsuarioRepository usuarioRepository;
    private final SituacaoAlunoService situacaoAlunoService;
    private final Clock clock;

    public CheckinService(CheckinRepository checkinRepository, UsuarioRepository usuarioRepository,
                          SituacaoAlunoService situacaoAlunoService, Clock clock) {
        this.checkinRepository = checkinRepository;
        this.usuarioRepository = usuarioRepository;
        this.situacaoAlunoService = situacaoAlunoService;
        this.clock = clock;
    }

    @Transactional
    public CheckinResponse registrar(CheckinRequest request) {
        Usuario aluno = usuarioRepository.findByIdAndPerfil(request.alunoId(), Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", request.alunoId()));
        situacaoAlunoService.exigirLiberado(aluno.getId());
        Checkin checkin = checkinRepository.save(new Checkin(aluno, LocalDateTime.now(clock)));
        return CheckinResponse.from(checkin);
    }
}
