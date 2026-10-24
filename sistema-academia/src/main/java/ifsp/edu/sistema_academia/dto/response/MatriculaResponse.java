package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.StatusMatricula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MatriculaResponse(Long id, Long alunoId, String alunoNome, PlanoResponse plano, LocalDate dataInicio,
                                LocalDate dataFim, StatusMatricula status, LocalDate dataEncerramento,
                                BigDecimal multaCancelamento, List<MensalidadeResponse> mensalidades) {

    public static MatriculaResponse from(Matricula m, LocalDate hoje) {
        return new MatriculaResponse(m.getId(), m.getAluno().getId(), m.getAluno().getNome(),
                PlanoResponse.from(m.getPlano()), m.getDataInicio(), m.getDataFim(), m.statusEfetivo(hoje),
                m.getDataEncerramento(), m.getMultaCancelamento(),
                m.getMensalidades().stream().map(men -> MensalidadeResponse.from(men, hoje)).toList());
    }
}