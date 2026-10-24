package ifsp.edu.sistema_academia.dto.response;

import java.math.BigDecimal;

/** Aluno com mensalidade(s) vencida(s) e não paga(s). O valor devido já inclui a multa de 2% por atraso. */
public record InadimplenteResponse(Long alunoId, String nome, String email, int mensalidadesVencidas,
                                   BigDecimal valorPrincipal, BigDecimal multa, BigDecimal valorDevido,
                                   long maiorAtrasoDias, boolean bloqueado) {
}