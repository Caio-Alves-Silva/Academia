package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusPresenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** {@code posicaoListaEspera} só é preenchida quando o status é LISTA_ESPERA (1 = próximo a ser chamado). */
public record InscricaoResponse(Long id, Long aulaId, Modalidade modalidade, Long alunoId, String alunoNome,
                                LocalDate dataAula, LocalTime horario, StatusInscricao status,
                                Long posicaoListaEspera, StatusPresenca presenca, LocalDateTime criadaEm) {

    public static InscricaoResponse from(Inscricao i, Long posicaoListaEspera) {
        return new InscricaoResponse(i.getId(), i.getAulaColetiva().getId(), i.getAulaColetiva().getModalidade(),
                i.getAluno().getId(), i.getAluno().getNome(), i.getDataAula(), i.getAulaColetiva().getHorario(),
                i.getStatus(), posicaoListaEspera, i.getPresenca(), i.getCriadaEm());
    }
}