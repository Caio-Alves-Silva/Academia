package ifsp.edu.sistema_academia.dto.request;

import jakarta.validation.constraints.NotNull;

public record TrocaPlanoRequest(@NotNull(message = "O novo plano é obrigatório") Long novoPlanoId) {
}