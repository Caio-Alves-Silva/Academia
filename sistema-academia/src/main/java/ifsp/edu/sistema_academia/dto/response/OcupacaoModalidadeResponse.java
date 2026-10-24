package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Modalidade;

import java.math.BigDecimal;

/** Ocupação média (em %) das ocorrências de aulas da modalidade no período. */
public record OcupacaoModalidadeResponse(Modalidade modalidade, int ocorrencias, BigDecimal ocupacaoMediaPercentual) {
}