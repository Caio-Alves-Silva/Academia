package ifsp.edu.sistema_academia.util;

import ifsp.edu.sistema_academia.util.Semana;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SemanaTest {

    @Test
    void semanaVaiDeSegundaADomingo() {
        Semana semana = Semana.de(LocalDate.of(2026, 3, 11)); // quarta

        assertThat(semana.inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(semana.fim()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void segundaEDomingoPertencemASuaPropriaSemana() {
        assertThat(Semana.de(LocalDate.of(2026, 3, 9)).inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(Semana.de(LocalDate.of(2026, 3, 15)).inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(Semana.de(LocalDate.of(2026, 3, 16)).inicio()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    void semanaAnteriorEContem() {
        Semana semana = Semana.de(LocalDate.of(2026, 3, 11));
        Semana anterior = semana.anterior();

        assertThat(anterior.inicio()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(anterior.fim()).isEqualTo(LocalDate.of(2026, 3, 8));
        assertThat(semana.contem(LocalDate.of(2026, 3, 9))).isTrue();
        assertThat(semana.contem(LocalDate.of(2026, 3, 15))).isTrue();
        assertThat(semana.contem(LocalDate.of(2026, 3, 8))).isFalse();
        assertThat(semana.contem(LocalDate.of(2026, 3, 16))).isFalse();
    }
}