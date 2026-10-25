package ifsp.edu.sistema_academia.model;

import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MensalidadeTest {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 3, 11);

    private Mensalidade mensalidade(String valor) {
        return new Mensalidade(null, 1, new BigDecimal(valor), VENCIMENTO, VENCIMENTO,
                VENCIMENTO.plusMonths(1).minusDays(1));
    }

    @Test
    @DisplayName("pagamento no dia do vencimento não tem multa")
    void semMultaNoVencimento() {
        Mensalidade m = mensalidade("100.00");

        assertThat(m.estaVencida(VENCIMENTO)).isFalse();
        assertThat(m.calcularMulta(VENCIMENTO)).isEqualByComparingTo("0.00");

        m.pagar(VENCIMENTO);

        assertThat(m.getStatus()).isEqualTo(StatusMensalidade.PAGA);
        assertThat(m.getValorPago()).isEqualByComparingTo("100.00");
        assertThat(m.getMulta()).isEqualByComparingTo("0.00");
        assertThat(m.getDataPagamento()).isEqualTo(VENCIMENTO);
    }

    @Test
    @DisplayName("pagamento após o vencimento tem multa de 2%")
    void multaDeDoisPorCento() {
        Mensalidade m = mensalidade("129.90");
        LocalDate hoje = VENCIMENTO.plusDays(1);

        assertThat(m.estaVencida(hoje)).isTrue();
        assertThat(m.diasEmAtraso(hoje)).isEqualTo(1);
        assertThat(m.valorAPagar(hoje)).isEqualByComparingTo("132.50");

        m.pagar(hoje);

        assertThat(m.getMulta()).isEqualByComparingTo("2.60");
        assertThat(m.getValorPago()).isEqualByComparingTo("132.50");
    }

    @Test
    @DisplayName("a multa incide sobre o valor já descontado")
    void multaSobreValorLiquido() {
        Mensalidade m = mensalidade("100.00");
        m.aplicarDesconto(new BigDecimal("20.00"));

        assertThat(m.valorLiquido()).isEqualByComparingTo("80.00");
        assertThat(m.calcularMulta(VENCIMENTO.plusDays(3))).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("desconto nunca passa do valor da parcela nem fica negativo")
    void descontoLimitado() {
        Mensalidade m = mensalidade("100.00");

        m.aplicarDesconto(new BigDecimal("250.00"));
        assertThat(m.getDesconto()).isEqualByComparingTo("100.00");

        m.aplicarDesconto(new BigDecimal("-5"));
        assertThat(m.getDesconto()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("bloqueia o aluno somente com mais de 5 dias de atraso")
    void bloqueioAposCincoDias() {
        Mensalidade m = mensalidade("100.00");

        assertThat(m.bloqueiaAluno(VENCIMENTO.plusDays(5))).isFalse();
        assertThat(m.bloqueiaAluno(VENCIMENTO.plusDays(6))).isTrue();
    }

    @Test
    @DisplayName("mensalidade paga ou cancelada não bloqueia nem está vencida")
    void pagaNaoBloqueia() {
        Mensalidade paga = mensalidade("100.00");
        paga.pagar(VENCIMENTO);
        Mensalidade cancelada = mensalidade("100.00");
        cancelada.cancelar();
        LocalDate muitoDepois = VENCIMENTO.plusMonths(2);

        assertThat(paga.bloqueiaAluno(muitoDepois)).isFalse();
        assertThat(paga.estaVencida(muitoDepois)).isFalse();
        assertThat(paga.diasEmAtraso(muitoDepois)).isZero();
        assertThat(cancelada.bloqueiaAluno(muitoDepois)).isFalse();
    }

    @Test
    @DisplayName("valor proporcional aos dias não usados do mês pago")
    void valorNaoUtilizado() {
        Mensalidade m = mensalidade("300.00"); // período 11/03 a 10/04 = 31 dias
        m.pagar(VENCIMENTO);

        // em 21/03 restam 20 dias (22/03 a 10/04): 300 * 20 / 31
        assertThat(m.valorNaoUtilizado(LocalDate.of(2026, 3, 21))).isEqualByComparingTo("193.55");
        // no último dia do período não sobra nada
        assertThat(m.valorNaoUtilizado(LocalDate.of(2026, 4, 10))).isEqualByComparingTo("0.00");
        // no primeiro dia só ele foi usado: 30 dias restantes
        assertThat(m.valorNaoUtilizado(VENCIMENTO)).isEqualByComparingTo("290.32");
    }

    @Test
    @DisplayName("parcela paga adiantada e ainda não iniciada volta inteira como crédito")
    void parcelaFuturaPagaVoltaInteira() {
        Mensalidade m = mensalidade("300.00");
        m.pagar(VENCIMENTO);

        assertThat(m.valorNaoUtilizado(VENCIMENTO.minusDays(10))).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("parcela não paga não gera crédito")
    void naoPagaSemCredito() {
        assertThat(mensalidade("300.00").valorNaoUtilizado(VENCIMENTO)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("só mensalidades pendentes podem ser pagas ou canceladas")
    void transicoesInvalidas() {
        Mensalidade paga = mensalidade("100.00");
        paga.pagar(VENCIMENTO);

        assertThatThrownBy(() -> paga.pagar(VENCIMENTO)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(paga::cancelar).isInstanceOf(IllegalStateException.class);
    }
}