package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusMensalidade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mensalidade. {@code valorAPagarHoje} só é preenchido para mensalidades pendentes e já inclui a multa de 2% se
 * a parcela estiver vencida.
 */
public record MensalidadeResponse(Long id, Long matriculaId, int numero, BigDecimal valor, BigDecimal desconto,
                                  BigDecimal valorLiquido, LocalDate vencimento, LocalDate periodoInicio,
                                  LocalDate periodoFim, StatusMensalidade status, LocalDate dataPagamento,
                                  BigDecimal multa, BigDecimal valorPago, long diasEmAtraso,
                                  BigDecimal valorAPagarHoje) {

    public static MensalidadeResponse from(Mensalidade m, LocalDate hoje) {
        return new MensalidadeResponse(m.getId(), m.getMatricula().getId(), m.getNumero(), m.getValor(),
                m.getDesconto(), m.valorLiquido(), m.getVencimento(), m.getPeriodoInicio(), m.getPeriodoFim(),
                m.getStatus(), m.getDataPagamento(), m.getMulta(), m.getValorPago(), m.diasEmAtraso(hoje),
                m.estaPendente() ? m.valorAPagar(hoje) : null);
    }
}