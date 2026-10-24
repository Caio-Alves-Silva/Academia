package ifsp.edu.sistema_academia.dto.request;

import jakarta.validation.constraints.NotNull;

public record MatriculaRequest(
        @NotNull(message = "O aluno é obrigatório") Long alunoId,
        @NotNull(message = "O plano é obrigatório") Long planoId) {
}