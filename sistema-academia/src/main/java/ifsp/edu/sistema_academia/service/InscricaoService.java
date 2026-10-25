package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.InscricaoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoInscricaoResponse;
import ifsp.edu.sistema_academia.dto.response.InscricaoResponse;
import ifsp.edu.sistema_academia.exception.BloqueioPorFaltasException;
import ifsp.edu.sistema_academia.exception.DataAulaInvalidaException;
import ifsp.edu.sistema_academia.exception.InscricaoDuplicadaException;
import ifsp.edu.sistema_academia.exception.InscricaoJaCanceladaException;
import ifsp.edu.sistema_academia.exception.LimiteSemanalAtingidoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.ModalidadeNaoIncluidaException;
import ifsp.edu.sistema_academia.exception.PrazoCancelamentoExpiradoException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.model.StatusPresenca;
import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.util.Semana;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF09 (inscrição com lista de espera) e RF10 (cancelamento com promoção automática da lista de espera).
 *
 * <p>Inscrições e cancelamentos de uma mesma aula são serializados pelo lock da linha da aula, de modo que duas
 * pessoas nunca ocupam a mesma última vaga. Cancelar e promover acontecem na mesma transação.
 */
@Service
public class InscricaoService {

    /** Antecedência mínima (em horas) para cancelar uma inscrição. */
    public static final long HORAS_ANTECEDENCIA_CANCELAMENTO = 2;
    /** Faltas na semana anterior que bloqueiam novas inscrições. */
    public static final long FALTAS_PARA_BLOQUEIO = 2;

    private final InscricaoRepository inscricaoRepository;
    private final AulaColetivaRepository aulaRepository;
    private final AlunoRepository alunoRepository;
    private final SituacaoAlunoService situacaoAlunoService;
    private final Clock clock;

    public InscricaoService(InscricaoRepository inscricaoRepository, AulaColetivaRepository aulaRepository,
                            AlunoRepository alunoRepository, SituacaoAlunoService situacaoAlunoService,
                            Clock clock) {
        this.inscricaoRepository = inscricaoRepository;
        this.aulaRepository = aulaRepository;
        this.alunoRepository = alunoRepository;
        this.situacaoAlunoService = situacaoAlunoService;
        this.clock = clock;
    }

    @Transactional
    public InscricaoResponse inscrever(Long aulaId, InscricaoRequest request, UsuarioAutenticado usuario) {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate data = request.data();

        Aluno aluno = alunoRepository.findByIdForUpdate(usuario.id())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", usuario.id()));
        AulaColetiva aula = aulaRepository.findByIdForUpdate(aulaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", aulaId));

        if (!aula.isAtiva()) {
            throw new RegraNegocioException("A aula " + aulaId + " está inativa");
        }
        validarDataDaAula(aula, data, agora);

        Matricula matricula = situacaoAlunoService.exigirLiberado(aluno.getId());
        if (data.isAfter(matricula.getDataFim())) {
            throw new MatriculaNaoAtivaException("A matrícula do aluno termina antes da data da aula");
        }
        validarFaltasDaSemanaAnterior(aluno.getId(), data);

        if (!matricula.getPlano().incluiModalidade(aula.getModalidade())) {
            throw new ModalidadeNaoIncluidaException(aula.getModalidade());
        }
        validarLimiteSemanal(aluno.getId(), matricula, data);

        if (inscricaoRepository.existsByAulaColetivaIdAndAlunoIdAndDataAulaAndStatusIn(
                aulaId, aluno.getId(), data, StatusInscricao.VIGENTES)) {
            throw new InscricaoDuplicadaException();
        }

        long confirmadas = inscricaoRepository.countByAulaColetivaIdAndDataAulaAndStatus(
                aulaId, data, StatusInscricao.CONFIRMADA);
        StatusInscricao status = confirmadas < aula.getCapacidade()
                ? StatusInscricao.CONFIRMADA
                : StatusInscricao.LISTA_ESPERA;
        Inscricao inscricao = inscricaoRepository.save(new Inscricao(aula, aluno, data, status, agora));
        return toResponse(inscricao);
    }

    /**
     * Cancela a inscrição (até 2 horas antes da aula). Se ela ocupava uma vaga, a vaga vai para o primeiro da
     * lista de espera que ainda estiver apto (matrícula ativa e sem bloqueio).
     */
    @Transactional
    public CancelamentoInscricaoResponse cancelar(Long inscricaoId, UsuarioAutenticado usuario) {
        LocalDateTime agora = LocalDateTime.now(clock);

        Long aulaId = inscricaoRepository.findAulaIdById(inscricaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Inscrição", inscricaoId));
        AulaColetiva aula = aulaRepository.findByIdForUpdate(aulaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", aulaId));
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Inscrição", inscricaoId));

        usuario.exigirAcessoAoAluno(inscricao.getAluno().getId());
        if (!inscricao.estaVigente()) {
            throw new InscricaoJaCanceladaException(inscricaoId);
        }
        LocalDateTime limite = aula.inicioEm(inscricao.getDataAula()).minusHours(HORAS_ANTECEDENCIA_CANCELAMENTO);
        if (agora.isAfter(limite)) {
            throw new PrazoCancelamentoExpiradoException();
        }

        boolean liberaVaga = inscricao.getStatus() == StatusInscricao.CONFIRMADA;
        inscricao.cancelar(agora);
        Inscricao promovida = liberaVaga ? promoverPrimeiroDaFila(aulaId, inscricao.getDataAula()) : null;

        return new CancelamentoInscricaoResponse(toResponse(inscricao),
                promovida == null ? null : toResponse(promovida));
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> listarPorAluno(Long alunoId, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(alunoId);
        alunoRepository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", alunoId));
        return inscricaoRepository.findByAlunoIdOrderByDataAulaDescIdDesc(alunoId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Inscrições vigentes (confirmadas e lista de espera, em ordem de chegada) de uma ocorrência da aula. */
    @Transactional(readOnly = true)
    public List<InscricaoResponse> listarPorOcorrencia(Long aulaId, LocalDate data) {
        AulaColetiva aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", aulaId));
        exigirDiaDaSemana(aula, data);
        return inscricaoRepository
                .findByAulaColetivaIdAndDataAulaAndStatusInOrderByCriadaEmAscIdAsc(aulaId, data, StatusInscricao.VIGENTES).stream()
                .map(this::toResponse)
                .toList();
    }

    private Inscricao promoverPrimeiroDaFila(Long aulaId, LocalDate data) {
        List<Inscricao> fila = inscricaoRepository.findByAulaColetivaIdAndDataAulaAndStatusOrderByCriadaEmAscIdAsc(
                aulaId, data, StatusInscricao.LISTA_ESPERA);
        for (Inscricao candidata : fila) {
            if (situacaoAlunoService.podeFrequentar(candidata.getAluno().getId())) {
                candidata.promover();
                return candidata;
            }
        }
        return null;
    }

    private void exigirDiaDaSemana(AulaColetiva aula, LocalDate data) {
        if (data.getDayOfWeek() != aula.getDiaSemana()) {
            throw new DataAulaInvalidaException("A aula acontece às " + aula.getDiaSemana()
                    + ", mas a data informada é " + data.getDayOfWeek());
        }
    }

    private void validarDataDaAula(AulaColetiva aula, LocalDate data, LocalDateTime agora) {
        exigirDiaDaSemana(aula, data);
        Semana semanaCorrente = Semana.de(agora.toLocalDate());
        if (!semanaCorrente.contem(data)) {
            throw new DataAulaInvalidaException("A inscrição só é permitida para aulas da semana corrente ("
                    + semanaCorrente.inicio() + " a " + semanaCorrente.fim() + ")");
        }
        if (!aula.inicioEm(data).isAfter(agora)) {
            throw new DataAulaInvalidaException("A aula de " + data + " às " + aula.getHorario()
                    + " já começou ou já aconteceu");
        }
    }

    /** RF11: 2 faltas em aulas inscritas na semana anterior impedem inscrições na semana corrente. */
    private void validarFaltasDaSemanaAnterior(Long alunoId, LocalDate data) {
        Semana anterior = Semana.de(data).anterior();
        long faltas = inscricaoRepository.countByAlunoIdAndDataAulaBetweenAndPresenca(
                alunoId, anterior.inicio(), anterior.fim(), StatusPresenca.FALTA);
        if (faltas >= FALTAS_PARA_BLOQUEIO) {
            throw new BloqueioPorFaltasException(faltas);
        }
    }

    private void validarLimiteSemanal(Long alunoId, Matricula matricula, LocalDate data) {
        Semana semana = Semana.de(data);
        long usadas = inscricaoRepository.countByAlunoIdAndDataAulaBetweenAndStatusIn(
                alunoId, semana.inicio(), semana.fim(), StatusInscricao.VIGENTES);
        int limite = matricula.getPlano().getLimiteAulasSemana();
        if (usadas >= limite) {
            throw new LimiteSemanalAtingidoException(limite);
        }
    }

    private InscricaoResponse toResponse(Inscricao inscricao) {
        Long posicao = null;
        if (inscricao.getStatus() == StatusInscricao.LISTA_ESPERA) {
            List<Inscricao> fila = inscricaoRepository
                    .findByAulaColetivaIdAndDataAulaAndStatusOrderByCriadaEmAscIdAsc(
                            inscricao.getAulaColetiva().getId(), inscricao.getDataAula(),
                            StatusInscricao.LISTA_ESPERA);
            posicao = (long) fila.indexOf(inscricao) + 1;
        }
        return InscricaoResponse.from(inscricao, posicao);
    }
}
