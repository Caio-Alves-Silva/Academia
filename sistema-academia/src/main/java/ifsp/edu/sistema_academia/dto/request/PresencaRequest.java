package ifsp.edu.sistema_academia.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PresencaRequest(
        @NotNull(message = "A data da aula é obrigatória")
        LocalDate data,

        @NotNull(message = "A lista de alunos presentes é obrigatória (pode ser vazia)")
        @Schema(description = "IDs dos alunos que compareceram. Os demais inscritos confirmados recebem falta.")
        List<@NotNull(message = "O ID do aluno não pode ser nulo") Long> alunosPresentes) {
}