package ifsp.edu.sistema_academia.model;

import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AulaColetivaTest {

    private final AulaColetiva aula = Fixtures.aula(1L, Fixtures.instrutor(1L), Modalidade.SPINNING,
            DayOfWeek.MONDAY, LocalTime.of(18, 0), 50, 15, LocalDate.of(2026, 3, 1));

    @Test
    void calculaHorarioFimEInicio() {
        assertThat(aula.horarioFim()).isEqualTo(LocalTime.of(18, 50));
        assertThat(aula.inicioEm(LocalDate.of(2026, 3, 9))).isEqualTo(LocalDateTime.of(2026, 3, 9, 18, 0));
    }

    @Test
    @DisplayName("horários que se cruzam no mesmo dia se sobrepõem")
    void sobreposicao() {
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(18, 0), 50)).isTrue();
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(18, 30), 50)).isTrue();
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(17, 30), 60)).isTrue();
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(18, 10), 15)).isTrue();
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(17, 0), 180)).isTrue();
    }

    @Test
    @DisplayName("aulas encostadas (uma termina quando a outra começa) não se sobrepõem")
    void aulasEncostadas() {
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(18, 50), 50)).isFalse();
        assertThat(aula.sobrepoe(DayOfWeek.MONDAY, LocalTime.of(17, 10), 50)).isFalse();
    }

    @Test
    @DisplayName("dias diferentes nunca se sobrepõem")
    void diasDiferentes() {
        assertThat(aula.sobrepoe(DayOfWeek.TUESDAY, LocalTime.of(18, 0), 50)).isFalse();
    }
}