package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse;
import ifsp.edu.sistema_academia.dto.response.InadimplenteResponse;
import ifsp.edu.sistema_academia.dto.response.OcupacaoModalidadeResponse;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 3, 11);

    @Mock
    MensalidadeRepository mensalidadeRepository;
    @Mock
    AulaColetivaRepository aulaRepository;
    @Mock
    InscricaoRepository inscricaoRepository;
    @Mock
    CheckinRepository checkinRepository;

    RelatorioService service;

    @BeforeEach
    void setUp() {
        service = new RelatorioService(mensalidadeRepository, aulaRepository, inscricaoRepository,
                checkinRepository, Fixtures.relogio(Fixtures.AGORA));
    }

    private InscricaoRepository.OcupacaoSessao sessao(long aulaId, LocalDate data, long total) {
        return new InscricaoRepository.OcupacaoSessao() {
            @Override
            public Long getAulaId() {
                return aulaId;
            }

            @Override
            public LocalDate getDataAula() {
                return data;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    @Test
    @DisplayName("inadimplentes: valor devido soma as parcelas vencidas e a multa de 2%")
    void inadimplentes() {
        Matricula m1 = Fixtures.matricula(1L, Fixtures.aluno(1L),
                Fixtures.plano(10L, 3, "100.00", 3, Modalidade.SPINNING), LocalDate.of(2026, 1, 11));
        Matricula m2 = Fixtures.matricula(2L, Fixtures.aluno(2L),
                Fixtures.plano(11L, 1, "100.00", 3, Modalidade.SPINNING), LocalDate.of(2026, 3, 8));
        List<Mensalidade> vencidas = List.of(
                m1.getMensalidades().get(0),  // 11/01: 59 dias
                m2.getMensalidades().get(0),  // 08/03: 3 dias
                m1.getMensalidades().get(1)); // 11/02: 28 dias
        when(mensalidadeRepository.findVencidas(StatusPagamento.PENDENTE, HOJE)).thenReturn(vencidas);

        List<InadimplenteResponse> resultado = service.inadimplentes();

        assertThat(resultado).hasSize(2);
        InadimplenteResponse primeiro = resultado.get(0); // maior valor devido primeiro
        assertThat(primeiro.alunoId()).isEqualTo(1L);
        assertThat(primeiro.mensalidadesVencidas()).isEqualTo(2);
        assertThat(primeiro.valorPrincipal()).isEqualByComparingTo("200.00");
        assertThat(primeiro.multa()).isEqualByComparingTo("4.00");
        assertThat(primeiro.valorDevido()).isEqualByComparingTo("204.00");
        assertThat(primeiro.maiorAtrasoDias()).isEqualTo(59);
        assertThat(primeiro.bloqueado()).isTrue();
        InadimplenteResponse segundo = resultado.get(1);
        assertThat(segundo.valorDevido()).isEqualByComparingTo("102.00");
        assertThat(segundo.maiorAtrasoDias()).isEqualTo(3);
        assertThat(segundo.bloqueado()).isFalse();
    }

    @Test
    void semInadimplentes() {
        assertThat(service.inadimplentes()).isEmpty();
    }

    @Test
    @DisplayName("ocupação média = média de confirmados/capacidade nas ocorrências já realizadas")
    void ocupacaoMedia() {
        LocalDate criada = LocalDate.of(2026, 3, 2);
        AulaColetiva spinning = Fixtures.aula(1L, Fixtures.instrutor(50L), Modalidade.SPINNING, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), 50, 10, criada);
        AulaColetiva yoga = Fixtures.aula(2L, Fixtures.instrutor(50L), Modalidade.YOGA, DayOfWeek.WEDNESDAY,
                LocalTime.of(19, 0), 60, 20, criada);
        AulaColetiva yogaManha = Fixtures.aula(3L, Fixtures.instrutor(51L), Modalidade.YOGA, DayOfWeek.WEDNESDAY,
                LocalTime.of(7, 0), 60, 10, criada);
        when(aulaRepository.findAll()).thenReturn(List.of(spinning, yoga, yogaManha));
        when(inscricaoRepository.contarPorSessao(StatusInscricao.CONFIRMADA, criada, HOJE)).thenReturn(List.of(
                sessao(1L, LocalDate.of(2026, 3, 2), 5),
                sessao(1L, LocalDate.of(2026, 3, 9), 10),
                sessao(3L, LocalDate.of(2026, 3, 4), 4),
                sessao(3L, LocalDate.of(2026, 3, 11), 10)));

        List<OcupacaoModalidadeResponse> resultado = service.ocupacaoPorModalidade(criada, HOJE);

        // SPINNING: segundas 02/03 (50%) e 09/03 (100%) -> 75%
        // YOGA: 04/03 19h (0/20 = 0%), 04/03 7h (4/10 = 40%) e 11/03 7h (10/10 = 100%) -> 46,67%;
        // a de 11/03 às 19h ainda não aconteceu e não entra na média
        assertThat(resultado).hasSize(2);
        OcupacaoModalidadeResponse s = resultado.stream().filter(r -> r.modalidade() == Modalidade.SPINNING)
                .findFirst().orElseThrow();
        OcupacaoModalidadeResponse y = resultado.stream().filter(r -> r.modalidade() == Modalidade.YOGA)
                .findFirst().orElseThrow();
        assertThat(s.ocorrencias()).isEqualTo(2);
        assertThat(s.ocupacaoMediaPercentual()).isEqualByComparingTo("75.00");
        assertThat(y.ocorrencias()).isEqualTo(3);
        assertThat(y.ocupacaoMediaPercentual()).isEqualByComparingTo("46.67");
    }

    @Test
    void ocupacaoNaoContaOcorrenciasAnterioresACriacaoDaAula() {
        AulaColetiva nova = Fixtures.aula(1L, Fixtures.instrutor(50L), Modalidade.SPINNING, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), 50, 10, LocalDate.of(2026, 3, 9));
        when(aulaRepository.findAll()).thenReturn(List.of(nova));

        List<OcupacaoModalidadeResponse> resultado = service.ocupacaoPorModalidade(null, null);

        assertThat(resultado).singleElement().satisfies(r -> {
            assertThat(r.ocorrencias()).isEqualTo(1);   // só 09/03
            assertThat(r.ocupacaoMediaPercentual()).isEqualByComparingTo("0.00");
        });
        verify(inscricaoRepository).contarPorSessao(StatusInscricao.CONFIRMADA, HOJE.minusDays(29), HOJE);
    }

    @Test
    void ocupacaoLimitaConfirmadasACapacidadeEValidaPeriodo() {
        AulaColetiva aula = Fixtures.aula(1L, Fixtures.instrutor(50L), Modalidade.SPINNING, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), 50, 10, LocalDate.of(2026, 3, 9));
        when(aulaRepository.findAll()).thenReturn(List.of(aula));
        when(inscricaoRepository.contarPorSessao(StatusInscricao.CONFIRMADA, LocalDate.of(2026, 3, 9), HOJE))
                .thenReturn(List.of(sessao(1L, LocalDate.of(2026, 3, 9), 14)));

        assertThat(service.ocupacaoPorModalidade(LocalDate.of(2026, 3, 9), HOJE).get(0).ocupacaoMediaPercentual())
                .isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> service.ocupacaoPorModalidade(HOJE, LocalDate.of(2026, 3, 1)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void frequenciaUsaOMesInformadoOuOCorrente() {
        List<FrequenciaAlunoResponse> esperado = List.of(new FrequenciaAlunoResponse(1L, "Maria", 12L));
        when(checkinRepository.frequenciaPorAluno(LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0))).thenReturn(esperado);
        when(checkinRepository.frequenciaPorAluno(LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 4, 1, 0, 0))).thenReturn(List.of());

        assertThat(service.frequenciaPorAluno(YearMonth.of(2026, 2))).isEqualTo(esperado);
        assertThat(service.frequenciaPorAluno(null)).isEmpty();
    }
}
