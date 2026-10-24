package ifsp.edu.sistema_academia.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PresencaResponse(Long aulaId, LocalDate dataAula, int presentes, int faltas,
                               List<InscricaoResponse> inscricoes) {
}