package ifsp.edu.sistema_academia.model;

import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 11);

    private Matricula matricula(int meses, String valor, LocalDate inicio) {
        Plano plano = Fixtures.plano(1L, meses, valor, 3, Modalidade.SPINNING);
        return Matricula.iniciar(Fixtures.aluno(1L), plano, inicio, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("gera uma mensalidade por mês, vencendo no mesmo dia")
    void geraMensalidades() {
        Matricula m = matricula(3, "100.00", INICIO);

        assertThat(m.getDataInicio()).isEqualTo(INICIO);
        assertThat(m.getDataFim()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(m.getStatusMatricula()).isEqualTo(StatusMatricula.ATIVA);
        assertThat(m.getMensalidades()).extracting(Mensalidade::getNumero).containsExactly(1, 2, 3);
        assertThat(m.getMensalidades()).extracting(Mensalidade::getVencimento)
                .containsExactly(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 4, 11), LocalDate.of(2026, 5, 11));
        assertThat(m.getMensalidades()).extracting(Mensalidade::getPeriodoFim)
                .containsExactly(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10), LocalDate.of(2026, 6, 10));
        assertThat(m.getMensalidades()).allSatisfy(men -> {
            assertThat(men.getValor()).isEqualByComparingTo("100.00");
            assertThat(men.getStatusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
        });
    }

    @Test
    @DisplayName("plano anual gera 12 mensalidades")
    void planoAnual() {
        Matricula m = matricula(12, "70.00", INICIO);

        assertThat(m.getMensalidades()).hasSize(12);
        assertThat(m.getDataFim()).isEqualTo(LocalDate.of(2027, 3, 10));
        assertThat(m.getMensalidades().get(11).getVencimento()).isEqualTo(LocalDate.of(2027, 2, 11));
    }

    @Test
    @DisplayName("início no dia 31 ajusta o vencimento ao último dia de meses mais curtos, sem acumular o desvio")
    void inicioNoDiaTrintaEUm() {
        Matricula m = matricula(3, "100.00", LocalDate.of(2026, 1, 31));

        assertThat(m.getMensalidades()).extracting(Mensalidade::getVencimento)
                .containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
        // os períodos são contíguos
        assertThat(m.getMensalidades().get(1).getPeriodoInicial())
                .isEqualTo(m.getMensalidades().get(0).getPeriodoFim().plusDays(1));
        assertThat(m.getMensalidades().get(2).getPeriodoInicial())
                .isEqualTo(m.getMensalidades().get(1).getPeriodoFim().plusDays(1));
    }

    @Test
    @DisplayName("desconto inicial é aplicado só na primeira mensalidade e limitado ao valor dela")
    void descontoNaPrimeira() {
        Plano plano = Fixtures.plano(1L, 3, "100.00", 3, Modalidade.SPINNING);

        Matricula m = Matricula.iniciar(Fixtures.aluno(1L), plano, INICIO, new BigDecimal("150.00"));

        assertThat(m.getMensalidades().get(0).getValor()).isEqualByComparingTo("0.00");
        assertThat(m.getMensalidades().get(1).getValor()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("ativa até o último dia de vigência; depois aparece como ENCERRADA")
    void vigencia() {
        Matricula m = matricula(1, "100.00", INICIO); // fim em 10/04

        assertThat(m.estaAtiva(LocalDate.of(2026, 4, 10))).isTrue();
        assertThat(m.statusEfetivo(LocalDate.of(2026, 4, 10))).isEqualTo(StatusMatricula.ATIVA);
        assertThat(m.estaAtiva(LocalDate.of(2026, 4, 11))).isFalse();
        assertThat(m.statusEfetivo(LocalDate.of(2026, 4, 11))).isEqualTo(StatusMatricula.ENCERRADA);
    }

    @Test
    @DisplayName("cancelamento de plano trimestral cancela as futuras e cobra 10% sobre elas")
    void cancelamentoComMulta() {
        Matricula m = matricula(3, "100.00", INICIO);
        m.getMensalidades().get(0).pagar(INICIO);

        List<Mensalidade> canceladas = m.cancelar(INICIO.plusDays(5));

        assertThat(canceladas).hasSize(2);
        assertThat(Matricula.valorTotal(canceladas)).isEqualByComparingTo("200.00");
        assertThat(m.getMultaCancelamento()).isEqualByComparingTo("20.00");
        assertThat(m.getStatusMatricula()).isEqualTo(StatusMatricula.CANCELADA);
        assertThat(m.getDataEncerramento()).isEqualTo(INICIO.plusDays(5));
        assertThat(m.getMensalidades()).extracting(Mensalidade::getStatusPagamento)
                .containsExactly(StatusPagamento.PAGO, StatusPagamento.CANCELADO, StatusPagamento.CANCELADO);
    }

    @Test
    @DisplayName("cancelamento de plano anual cobra 10% das 11 mensalidades futuras")
    void cancelamentoAnual() {
        Matricula m = matricula(12, "70.00", INICIO);

        m.cancelar(INICIO);

        assertThat(m.getMultaCancelamento()).isEqualByComparingTo("77.00"); // 10% de 11 x 70
    }

    @Test
    @DisplayName("cancelamento de plano mensal não tem multa")
    void cancelamentoMensalSemMulta() {
        Matricula m = matricula(1, "100.00", INICIO);

        List<Mensalidade> canceladas = m.cancelar(INICIO);

        assertThat(canceladas).isEmpty();
        assertThat(m.getMultaCancelamento()).isEqualByComparingTo("0.00");
        assertThat(m.getStatusMatricula()).isEqualTo(StatusMatricula.CANCELADA);
    }

    @Test
    @DisplayName("mensalidades vencidas ou do dia continuam pendentes após o cancelamento")
    void vencidasNaoSaoCanceladas() {
        Matricula m = matricula(3, "100.00", INICIO);

        List<Mensalidade> canceladas = m.cancelar(LocalDate.of(2026, 4, 11)); // vencimento da 2ª é hoje

        assertThat(canceladas).hasSize(1);
        assertThat(m.getMensalidades()).extracting(Mensalidade::getStatusPagamento)
                .containsExactly(StatusPagamento.PENDENTE, StatusPagamento.PENDENTE,
                        StatusPagamento.CANCELADO);
    }

    @Test
    @DisplayName("troca de plano encerra a matrícula e cancela as mensalidades futuras")
    void encerramentoPorTroca() {
        Matricula m = matricula(3, "300.00", INICIO);
        m.getMensalidades().get(0).pagar(INICIO);
        LocalDate hoje = LocalDate.of(2026, 3, 21);

        BigDecimal credito = m.creditoNaoUtilizado(hoje);
        List<Mensalidade> canceladas = m.encerrarPorTroca(hoje);

        assertThat(credito).isEqualByComparingTo("193.55");
        assertThat(canceladas).hasSize(2);
        assertThat(m.getStatusMatricula()).isEqualTo(StatusMatricula.ENCERRADA);
        assertThat(m.getMultaCancelamento()).isEqualByComparingTo("0");
        assertThat(m.estaAtiva(hoje)).isFalse();
    }

    @Test
    @DisplayName("crédito é zero quando o mês corrente não foi pago")
    void creditoZeroSemPagamento() {
        Matricula m = matricula(3, "300.00", INICIO);

        assertThat(m.creditoNaoUtilizado(LocalDate.of(2026, 3, 21))).isEqualByComparingTo("0.00");
    }
}
