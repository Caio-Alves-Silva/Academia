package ifsp.edu.sistema_academia.dto.response;

/** Inscrição cancelada e, se havia lista de espera, a inscrição que herdou a vaga. */
public record CancelamentoInscricaoResponse(InscricaoResponse inscricaoCancelada,
                                            InscricaoResponse inscricaoPromovida) {
}