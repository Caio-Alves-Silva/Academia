package ifsp.edu.sistema_academia.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckinRequest(@NotNull(message = "O aluno é obrigatório") Long alunoId) {
}