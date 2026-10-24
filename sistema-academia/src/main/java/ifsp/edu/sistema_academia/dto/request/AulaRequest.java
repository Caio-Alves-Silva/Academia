package ifsp.edu.sistema_academia.dto.request;

import ifsp.edu.sistema_academia.model.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AulaRequest(
        @NotNull(message = "A modalidade é obrigatória")
        Modalidade modalidade,

        @NotNull(message = "O instrutor é obrigatório")
        Long instrutorId,

        @NotNull(message = "O dia da semana é obrigatório")
        @Schema(example = "MONDAY")
        DayOfWeek diaSemana,

        @NotNull(message = "O horário é obrigatório")
        @Schema(type = "string", example = "18:00")
        LocalTime horario,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 15, message = "A duração mínima é de 15 minutos")
        @Max(value = 240, message = "A duração máxima é de 240 minutos")
        @Schema(example = "50")
        Integer duracaoMinutos,

        @NotNull(message = "A capacidade é obrigatória")
        @Min(value = 1, message = "A capacidade mínima é 1")
        @Max(value = 200, message = "A capacidade máxima é 200")
        @Schema(example = "15")
        Integer capacidade) {
}