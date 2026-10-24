package ifsp.edu.sistema_academia.dto.response;

import java.math.BigDecimal;

/** Resultado do cancelamento: mensalidades futuras canceladas e multa (10% em planos de 3 ou 12 meses). */
public record CancelamentoMatriculaResponse(MatriculaResponse matricula, int mensalidadesCanceladas,
                                            BigDecimal valorMensalidadesCanceladas, BigDecimal multaCancelamento) {
}