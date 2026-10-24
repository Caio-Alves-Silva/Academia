package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Registro de entrada de um aluno na academia. */
@Entity
@Table(name = "checkins", indexes = @Index(name = "idx_checkins_aluno_data", columnList = "aluno_id, data_hora"))
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected Checkin() {
    }

    public Checkin(Usuario aluno, LocalDateTime dataHora) {
        this.aluno = aluno;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
