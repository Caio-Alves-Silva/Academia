package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Tentativa de entrada de um aluno na academia (liberada ou recusada, com o motivo do bloqueio). */
@Entity
@Table(name = "checkin", indexes = @Index(name = "idx_checkin_aluno_data", columnList = "aluno_id, data_hora_entrada"))
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @Column(name = "data_hora_entrada", nullable = false)
    private LocalDateTime dataHoraEntrada;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_acesso", nullable = false, length = 20)
    private TipoAcesso tipoAcesso;

    @Column(name = "acesso_liberado", nullable = false)
    private Boolean acessoLiberado;

    @Column(name = "motivo_bloqueio", length = 255)
    private String motivoBloqueio;

    protected Checkin() {
    }

    public Checkin(Aluno aluno, LocalDateTime dataHoraEntrada, TipoAcesso tipoAcesso, Boolean acessoLiberado,
                   String motivoBloqueio) {
        this.aluno = aluno;
        this.dataHoraEntrada = dataHoraEntrada;
        this.tipoAcesso = tipoAcesso;
        this.acessoLiberado = acessoLiberado;
        this.motivoBloqueio = motivoBloqueio;
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public TipoAcesso getTipoAcesso() {
        return tipoAcesso;
    }

    public Boolean getAcessoLiberado() {
        return acessoLiberado;
    }

    public String getMotivoBloqueio() {
        return motivoBloqueio;
    }
}
