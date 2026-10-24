package ifsp.edu.sistema_academia.dto.request;

import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.validation.DuracaoPlanoValida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record PlanoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
        @Schema(example = "Trimestral Completo")
        String nome,

        @NotNull(message = "A duração é obrigatória")
        @DuracaoPlanoValida
        @Schema(example = "3", allowableValues = {"1", "3", "12"})
        Integer duracaoMeses,

        @NotNull(message = "O valor mensal é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mensal deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O valor mensal deve ter no máximo 2 casas decimais")
        @Schema(example = "129.90")
        BigDecimal valorMensal,

        @NotEmpty(message = "Informe ao menos uma modalidade")
        Set<Modalidade> modalidades,

        @NotNull(message = "O limite de aulas coletivas por semana é obrigatório")
        @Min(value = 0, message = "O limite semanal não pode ser negativo")
        @Max(value = 50, message = "O limite semanal deve ser no máximo 50")
        @Schema(example = "3", description = "0 = o plano não permite aulas coletivas")
        Integer limiteAulasSemana) {
}