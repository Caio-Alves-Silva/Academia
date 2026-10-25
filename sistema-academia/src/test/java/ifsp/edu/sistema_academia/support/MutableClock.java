package ifsp.edu.sistema_academia.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio controlável pelos testes funcionais: permite "viajar" no tempo (vencimentos, semanas, prazos). */
public class MutableClock extends Clock {

    private final ZoneId zona;
    private volatile Instant instante;

    public MutableClock(ZoneId zona) {
        this.zona = zona;
        this.instante = Instant.now();
    }

    public void definir(LocalDateTime dataHora) {
        this.instante = dataHora.atZone(zona).toInstant();
    }

    public void avancar(Duration duracao) {
        this.instante = instante.plus(duracao);
    }

    public void avancarDias(long dias) {
        avancar(Duration.ofDays(dias));
    }

    public void avancarHoras(long horas) {
        avancar(Duration.ofHours(horas));
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return Clock.fixed(instante, novaZona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}