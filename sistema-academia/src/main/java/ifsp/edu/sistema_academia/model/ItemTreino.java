package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Exercício de uma {@link FichaTreino} (composição: não existe sem a ficha). */
@Entity
@Table(name = "item_treino")
public class ItemTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Long getId() {
        return id;
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
