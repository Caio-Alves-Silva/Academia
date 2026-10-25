package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.MensalidadeResponse;
import ifsp.edu.sistema_academia.exception.MensalidadeCanceladaException;
import ifsp.edu.sistema_academia.exception.MensalidadeJaPagaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
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
    private final AlunoRepository alunoRepository;
    private final Clock clock;

    public MensalidadeService(MensalidadeRepository mensalidadeRepository, AlunoRepository alunoRepository,
                              Clock clock) {
        this.mensalidadeRepository = mensalidadeRepository;
        this.alunoRepository = alunoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponse> listarPorAluno(Long alunoId, StatusPagamento status,
                                                    UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        alunoRepository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        LocalDate hoje = LocalDate.now(clock);
        List<Mensalidade> mensalidades = status == null
                ? mensalidadeRepository.findByMatriculaAlunoIdOrderByVencimentoAscIdAsc(alunoId)
                : mensalidadeRepository.findByMatriculaAlunoIdAndStatusPagamentoOrderByVencimentoAscIdAsc(alunoId, status);
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
        if (mensalidade.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new MensalidadeJaPagaException(id);
        }
        if (mensalidade.getStatusPagamento() == StatusPagamento.CANCELADO) {
            throw new MensalidadeCanceladaException(id);
        }
        mensalidade.pagar(hoje);
        return MensalidadeResponse.from(mensalidade, hoje);
    }
}
