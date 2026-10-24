package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse;
import ifsp.edu.sistema_academia.dto.response.InadimplenteResponse;
import ifsp.edu.sistema_academia.dto.response.OcupacaoModalidadeResponse;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RF14 (inadimplentes), RF15 (ocupação média por modalidade) e RF16 (frequência por aluno no mês). */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    /** Período padrão do relatório de ocupação, em dias, terminando hoje. */
    public static final int PERIODO_PADRAO_DIAS = 30;

    private final MensalidadeRepository mensalidadeRepository;
    private final AulaColetivaRepository aulaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final CheckinRepository checkinRepository;
    private final Clock clock;

    public RelatorioService(MensalidadeRepository mensalidadeRepository, AulaColetivaRepository aulaRepository,
                            InscricaoRepository inscricaoRepository, CheckinRepository checkinRepository,
                            Clock clock) {
        this.mensalidadeRepository = mensalidadeRepository;
        this.aulaRepository = aulaRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.checkinRepository = checkinRepository;
        this.clock = clock;
    }

    /** Alunos com mensalidade vencida e não paga. O valor devido inclui a multa de 2% de cada parcela vencida. */
    public List<InadimplenteResponse> inadimplentes() {
        LocalDate hoje = LocalDate.now(clock);
        Map<Long, List<Mensalidade>> porAluno = new LinkedHashMap<>();
        for (Mensalidade m : mensalidadeRepository.findVencidas(StatusMensalidade.PENDENTE, hoje)) {
            porAluno.computeIfAbsent(m.getMatricula().getAluno().getId(), id -> new ArrayList<>()).add(m);
        }
        return porAluno.values().stream()
                .map(vencidas -> montarInadimplente(vencidas, hoje))
                .sorted(Comparator.comparing(InadimplenteResponse::valorDevido).reversed()
                        .thenComparing(InadimplenteResponse::nome))
                .toList();
    }

    /**
     * Ocupação média por modalidade: média, sobre todas as ocorrências (aula + data) do período já iniciadas, de
     * inscrições confirmadas / capacidade. Ocorrências sem nenhuma inscrição contam como 0%.
     */
    public List<OcupacaoModalidadeResponse> ocupacaoPorModalidade(LocalDate inicio, LocalDate fim) {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate hoje = agora.toLocalDate();
        LocalDate fimEfetivo = fim == null ? hoje : fim;
        LocalDate inicioEfetivo = inicio == null ? fimEfetivo.minusDays(PERIODO_PADRAO_DIAS - 1L) : inicio;
        if (inicioEfetivo.isAfter(fimEfetivo)) {
            throw new RegraNegocioException("A data de início não pode ser posterior à data de fim");
        }

        Map<String, Long> confirmadasPorOcorrencia = new HashMap<>();
        inscricaoRepository.contarPorSessao(StatusInscricao.CONFIRMADA, inicioEfetivo, fimEfetivo)
                .forEach(o -> confirmadasPorOcorrencia.put(chave(o.getAulaId(), o.getDataAula()), o.getTotal()));

        Map<Modalidade, BigDecimal> somaOcupacao = new EnumMap<>(Modalidade.class);
        Map<Modalidade, Integer> ocorrencias = new EnumMap<>(Modalidade.class);

        for (AulaColetiva aula : aulaRepository.findAll()) {
            LocalDate primeira = inicioEfetivo.isBefore(aula.getCriadaEm()) ? aula.getCriadaEm() : inicioEfetivo;
            LocalDate data = primeira.with(TemporalAdjusters.nextOrSame(aula.getDiaSemana()));
            for (; !data.isAfter(fimEfetivo); data = data.plusWeeks(1)) {
                if (aula.inicioEm(data).isAfter(agora)) {
                    continue; // ocorrência futura: ainda não aconteceu
                }
                long confirmadas = confirmadasPorOcorrencia.getOrDefault(chave(aula.getId(), data), 0L);
                BigDecimal taxa = BigDecimal.valueOf(Math.min(confirmadas, aula.getCapacidade()))
                        .divide(BigDecimal.valueOf(aula.getCapacidade()), 6, RoundingMode.HALF_UP);
                somaOcupacao.merge(aula.getModalidade(), taxa, BigDecimal::add);
                ocorrencias.merge(aula.getModalidade(), 1, Integer::sum);
            }
        }

        return ocorrencias.entrySet().stream()
                .map(e -> new OcupacaoModalidadeResponse(e.getKey(), e.getValue(),
                        somaOcupacao.get(e.getKey())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(e.getValue()), 2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(OcupacaoModalidadeResponse::modalidade))
                .toList();
    }

    /** Entradas de cada aluno no mês informado (padrão: mês corrente), incluindo quem não entrou nenhuma vez. */
    public List<FrequenciaAlunoResponse> frequenciaPorAluno(YearMonth mes) {
        YearMonth mesEfetivo = mes == null ? YearMonth.now(clock) : mes;
        return checkinRepository.frequenciaPorAluno(
                mesEfetivo.atDay(1).atStartOfDay(), mesEfetivo.plusMonths(1).atDay(1).atStartOfDay());
    }

    private InadimplenteResponse montarInadimplente(List<Mensalidade> vencidas, LocalDate hoje) {
        Usuario aluno = vencidas.get(0).getMatricula().getAluno();
        BigDecimal principal = vencidas.stream().map(Mensalidade::valorLiquido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal multa = vencidas.stream().map(m -> m.calcularMulta(hoje))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long maiorAtraso = vencidas.stream().mapToLong(m -> m.diasEmAtraso(hoje)).max().orElse(0);
        boolean bloqueado = vencidas.stream().anyMatch(m -> m.bloqueiaAluno(hoje));
        return new InadimplenteResponse(aluno.getId(), aluno.getNome(), aluno.getEmail(), vencidas.size(),
                principal, multa, principal.add(multa), maiorAtraso, bloqueado);
    }

    private static String chave(Long aulaId, LocalDate data) {
        return aulaId + "|" + data;
    }
}
