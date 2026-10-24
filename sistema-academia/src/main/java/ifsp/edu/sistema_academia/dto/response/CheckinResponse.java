package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Checkin;

import java.time.LocalDateTime;

public record CheckinResponse(Long id, Long alunoId, String alunoNome, LocalDateTime dataHora) {

    public static CheckinResponse from(Checkin checkin) {
        return new CheckinResponse(checkin.getId(), checkin.getAluno().getId(), checkin.getAluno().getNome(),
                checkin.getDataHora());
    }
}