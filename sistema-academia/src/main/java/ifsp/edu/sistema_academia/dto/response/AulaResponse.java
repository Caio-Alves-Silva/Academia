package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Modalidade;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AulaResponse(Long id, Modalidade modalidade, Long instrutorId, String instrutorNome,
                           DayOfWeek diaSemana, LocalTime horario, LocalTime horarioFim, int duracaoMinutos,
                           int capacidade, boolean ativa) {

    public static AulaResponse from(AulaColetiva aula) {
        return new AulaResponse(aula.getId(), aula.getModalidade(), aula.getInstrutor().getId(),
                aula.getInstrutor().getNome(), aula.getDiaSemana(), aula.getHorario(), aula.horarioFim(),
                aula.getDuracaoMinutos(), aula.getCapacidade(), aula.isAtiva());
    }
}