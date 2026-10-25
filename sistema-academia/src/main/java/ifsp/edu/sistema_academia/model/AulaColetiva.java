package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Aula coletiva recorrente (toda semana, no mesmo dia e horário). */
@Entity
@Table(name = "aula_coletiva")
public class AulaColetiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    private Instrutor instrutor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(nullable = false)
    private int capacidade;

    @Column(name = "criada_em", nullable = false)
    private LocalDate criadaEm;

    @Column(nullable = false)
    private boolean ativa = true;

    @OneToMany(mappedBy = "aulaColetiva")
    private List<Inscricao> inscricoes = new ArrayList<>();

    protected AulaColetiva() {
    }

    public AulaColetiva(Modalidade modalidade, Instrutor instrutor, DayOfWeek diaSemana, LocalTime horario,
                        int duracaoMinutos, int capacidade, LocalDate criadaEm) {
        this.modalidade = modalidade;
        this.instrutor = instrutor;
        this.diaSemana = diaSemana;
        this.horario = horario;
        this.duracaoMinutos = duracaoMinutos;
        this.capacidade = capacidade;
        this.criadaEm = criadaEm;
    }

    public LocalTime horarioFim() {
        return horario.plusMinutes(duracaoMinutos);
    }

    /** Verdadeiro se esta aula ocupa algum instante em comum com o intervalo informado, no mesmo dia da semana. */
    public boolean sobrepoe(DayOfWeek dia, LocalTime inicio, int duracaoEmMinutos) {
        if (diaSemana != dia) {
            return false;
        }
        LocalTime fim = inicio.plusMinutes(duracaoEmMinutos);
        return inicio.isBefore(horarioFim()) && horario.isBefore(fim);
    }

    /** Instante em que a ocorrência desta aula na {@code data} começa. */
    public LocalDateTime inicioEm(LocalDate data) {
        return LocalDateTime.of(data, horario);
    }

    public void desativar() {
        this.ativa = false;
    }

    public Long getId() {
        return id;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public Instrutor getInstrutor() {
        return instrutor;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public LocalDate getCriadaEm() {
        return criadaEm;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }
}
