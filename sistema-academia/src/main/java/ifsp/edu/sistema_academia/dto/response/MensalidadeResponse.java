package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mensalidade. {@code valor} já considera o desconto de troca de plano; {@code valorAPagarHoje} só é preenchido
 * para mensalidades pendentes e já inclui a multa de 2% se a parcela estiver vencida.
 */
public record MensalidadeResponse(Long id, Long matriculaId, int numero, BigDecimal valor, LocalDate vencimento,
                                  LocalDate periodoInicial, LocalDate periodoFim, StatusPagamento status,
                                  LocalDate dataPagamento, BigDecimal multa, BigDecimal valorPago,
                                  long diasEmAtraso, BigDecimal valorAPagarHoje) {

    public static MensalidadeResponse from(Mensalidade m, LocalDate hoje) {
        return new MensalidadeResponse(m.getId(), m.getMatricula().getId(), m.getNumero(), m.getValor(),
                m.getVencimento(), m.getPeriodoInicial(), m.getPeriodoFim(), m.getStatusPagamento(),
                m.getDataPagamento(), m.multaPaga(), m.getValorPago(), m.diasEmAtraso(hoje),
                m.estaPendente() ? m.valorAPagar(hoje) : null);
    }
}