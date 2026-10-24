package ifsp.edu.sistema_academia.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InscricaoRequest(
        @NotNull(message = "A data da aula é obrigatória")
        @Schema(example = "2026-03-11", description = "Data da ocorrência, dentro da semana corrente (segunda a domingo)")
        LocalDate data) {
}