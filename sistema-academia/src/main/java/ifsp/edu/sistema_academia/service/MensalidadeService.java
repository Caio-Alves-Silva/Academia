package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.MensalidadeResponse;
import ifsp.edu.sistema_academia.exception.MensalidadeCanceladaException;
import ifsp.edu.sistema_academia.exception.MensalidadeJaPagaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** RF05: consulta e pagamento de mensalidades (multa de 2% após o vencimento). */
@Service
public class MensalidadeService {

    private final MensalidadeRepository mensalidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public MensalidadeService(MensalidadeRepository mensalidadeRepository, UsuarioRepository usuarioRepository,
                              Clock clock) {
        this.mensalidadeRepository = mensalidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponse> listarPorAluno(Long alunoId, StatusMensalidade status,
                                                    UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        usuarioRepository.findByIdAndPerfil(alunoId, Perfil.ALUNO)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        LocalDate hoje = LocalDate.now(clock);
        List<Mensalidade> mensalidades = status == null
                ? mensalidadeRepository.findByMatriculaAlunoIdOrderByVencimentoAscIdAsc(alunoId)
                : mensalidadeRepository.findByMatriculaAlunoIdAndStatusOrderByVencimentoAscIdAsc(alunoId, status);
        return mensalidades.stream().map(m -> MensalidadeResponse.from(m, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public MensalidadeResponse buscar(Long id, UsuarioAutenticado solicitante) {
        Mensalidade mensalidade = mensalidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Mensalidade", id));
        solicitante.exigirAcessoAoAluno(mensalidade.getMatricula().getAluno().getId());
        return MensalidadeResponse.from(mensalidade, LocalDate.now(clock));
    }

    /** Registra o pagamento. Após o vencimento, soma multa de 2% sobre o valor da parcela. */
    @Transactional
    public MensalidadeResponse pagar(Long id, UsuarioAutenticado solicitante) {
        LocalDate hoje = LocalDate.now(clock);
        Mensalidade mensalidade = mensalidadeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Mensalidade", id));
        solicitante.exigirAcessoAoAluno(mensalidade.getMatricula().getAluno().getId());
        if (mensalidade.getStatus() == StatusMensalidade.PAGA) {
            throw new MensalidadeJaPagaException(id);
        }
        if (mensalidade.getStatus() == StatusMensalidade.CANCELADA) {
            throw new MensalidadeCanceladaException(id);
        }
        mensalidade.pagar(hoje);
        return MensalidadeResponse.from(mensalidade, hoje);
    }
}
