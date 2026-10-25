package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Checkin;
import ifsp.edu.sistema_academia.model.TipoAcesso;

import java.time.LocalDateTime;

public record CheckinResponse(Long id, Long alunoId, String alunoNome, LocalDateTime dataHoraEntrada,
                              TipoAcesso tipoAcesso, boolean acessoLiberado, String motivoBloqueio) {

    public static CheckinResponse from(Checkin checkin) {
        return new CheckinResponse(checkin.getId(), checkin.getAluno().getId(), checkin.getAluno().getNome(),
                checkin.getDataHoraEntrada(), checkin.getTipoAcesso(), checkin.getAcessoLiberado(),
                checkin.getMotivoBloqueio());
    }
}