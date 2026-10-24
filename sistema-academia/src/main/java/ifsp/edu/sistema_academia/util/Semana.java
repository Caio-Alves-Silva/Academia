package ifsp.edu.sistema_academia.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Semana de segunda a domingo (ISO) que contém uma data. */
public record Semana(LocalDate inicio, LocalDate fim) {

    public static Semana de(LocalDate data) {
        LocalDate segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Semana(segunda, segunda.plusDays(6));
    }

    public Semana anterior() {
        return new Semana(inicio.minusWeeks(1), fim.minusWeeks(1));
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }
}