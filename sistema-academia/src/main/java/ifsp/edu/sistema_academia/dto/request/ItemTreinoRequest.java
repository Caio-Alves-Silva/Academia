package ifsp.edu.sistema_academia.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemTreinoRequest(
        @NotBlank(message = "O exercício é obrigatório")
        @Size(max = 120, message = "O exercício deve ter no máximo 120 caracteres")
        @Schema(example = "Supino reto")
        String exercicio,

        @NotNull(message = "O número de séries é obrigatório")
        @Min(value = 1, message = "O mínimo é 1 série")
        @Max(value = 20, message = "O máximo é 20 séries")
        @Schema(example = "4")
        Integer series,

        @NotNull(message = "O número de repetições é obrigatório")
        @Min(value = 1, message = "O mínimo é 1 repetição")
        @Max(value = 200, message = "O máximo é 200 repetições")
        @Schema(example = "12")
        Integer repeticoes,

        @NotNull(message = "A carga é obrigatória (use 0 para exercícios sem carga)")
        @DecimalMin(value = "0.0", message = "A carga não pode ser negativa")
        @Digits(integer = 4, fraction = 2, message = "A carga deve ter no máximo 2 casas decimais")
        @Schema(example = "40.0")
        BigDecimal cargaKg) {
}