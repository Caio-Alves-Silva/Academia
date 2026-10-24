package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** Exercício de uma {@link FichaTreino}. */
@Embeddable
public class ItemTreino {

    @Column(nullable = false, length = 120)
    private String exercicio;

    @Column(nullable = false)
    private int series;

    @Column(nullable = false)
    private int repeticoes;

    @Column(name = "carga_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal cargaKg;

    protected ItemTreino() {
    }

    public ItemTreino(String exercicio, int series, int repeticoes, BigDecimal cargaKg) {
        this.exercicio = exercicio;
        this.series = series;
        this.repeticoes = repeticoes;
        this.cargaKg = cargaKg;
    }

    public String getExercicio() {
        return exercicio;
    }

    public int getSeries() {
        return series;
    }

    public int getRepeticoes() {
        return repeticoes;
    }

    public BigDecimal getCargaKg() {
        return cargaKg;
    }
}
