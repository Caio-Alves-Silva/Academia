package ifsp.edu.sistema_academia.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Schema(example = "recepcao@academia.com")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(example = "Recepcao@123")
        String senha) {
}