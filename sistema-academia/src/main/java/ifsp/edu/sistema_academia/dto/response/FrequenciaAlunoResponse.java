package ifsp.edu.sistema_academia.dto.response;

/** Quantidade de entradas (check-ins) de um aluno em um mês. */
public record FrequenciaAlunoResponse(Long alunoId, String nome, Long entradas) {
}