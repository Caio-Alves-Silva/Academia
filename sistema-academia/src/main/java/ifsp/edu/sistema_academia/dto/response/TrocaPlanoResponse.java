package ifsp.edu.sistema_academia.dto.response;

import java.math.BigDecimal;

/**
 * Resultado da troca de plano: a matrícula encerrada, a nova matrícula e o desconto (crédito proporcional aos dias
 * não usados do mês já pago) aplicado na primeira mensalidade da nova matrícula.
 */
public record TrocaPlanoResponse(MatriculaResponse matriculaEncerrada, MatriculaResponse novaMatricula,
                                 BigDecimal creditoCalculado, BigDecimal descontoAplicado,
                                 int mensalidadesCanceladas) {
}