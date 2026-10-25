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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inscrição de um aluno em uma ocorrência (aula + data) de uma aula coletiva. A ordem da lista de espera é a
 * ordem de criação ({@code criadaEm}, desempatada pelo id).
 */
@Entity
@Table(name = "inscricao", indexes = {
        @Index(name = "idx_inscricao_sessao", columnList = "aula_id, data_aula, status"),
        @Index(name = "idx_inscricao_aluno", columnList = "aluno_id, data_aula")
})
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private AulaColetiva aulaColetiva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @Column(name = "data_aula", nullable = false)
    private LocalDate dataAula;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusInscricao status;

    /** PENDENTE até o instrutor registrar a lista de presença. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusPresenca presenca = StatusPresenca.PENDENTE;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    protected Inscricao() {
    }

    public Inscricao(AulaColetiva aulaColetiva, Aluno aluno, LocalDate dataAula, StatusInscricao status,
                     LocalDateTime criadaEm) {
        this.aulaColetiva = aulaColetiva;
        this.aluno = aluno;
        this.dataAula = dataAula;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    public boolean estaVigente() {
        return StatusInscricao.VIGENTES.contains(status);
    }

    public void cancelar(LocalDateTime agora) {
        this.status = StatusInscricao.CANCELADA;
        this.canceladaEm = agora;
    }

    /** Move o aluno da lista de espera para uma vaga confirmada. */
    public void promover() {
        this.status = StatusInscricao.CONFIRMADA;
    }

    public void registrarPresenca(boolean compareceu) {
        this.presenca = compareceu ? StatusPresenca.PRESENTE : StatusPresenca.FALTA;
    }

    public Long getId() {
        return id;
    }

    public AulaColetiva getAulaColetiva() {
        return aulaColetiva;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public LocalDate getDataAula() {
        return dataAula;
    }

    public StatusInscricao getStatus() {
        return status;
    }

    public StatusPresenca getPresenca() {
        return presenca;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }
}
