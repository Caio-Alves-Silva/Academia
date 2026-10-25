package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Instrutor: ministra aulas coletivas e monta fichas de treino. */
@Entity
@Table(name = "instrutor")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Instrutor extends Usuario {

    @Column(length = 20)
    private String cref;

    @Column(length = 100)
    private String especialidade;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Turno turno;

    @Column(name = "data_contratacao")
    private LocalDate dataContratacao;

    @Column(nullable = false)
    private Boolean ativo = Boolean.TRUE;

    @OneToMany(mappedBy = "instrutor")
    private List<AulaColetiva> aulas = new ArrayList<>();

    @OneToMany(mappedBy = "instrutor")
    private List<FichaTreino> fichasCriadas = new ArrayList<>();

    protected Instrutor() {
    }

    public Instrutor(String nome, String cpf, String email, String senhaHash, LocalDateTime criadoEm) {
        super(nome, cpf, email, senhaHash, Perfil.INSTRUTOR, criadoEm);
    }

    public String getCref() {
        return cref;
    }

    public void setCref(String cref) {
        this.cref = cref;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public LocalDate getDataContratacao() {
        return dataContratacao;
    }

    public void setDataContratacao(LocalDate dataContratacao) {
        this.dataContratacao = dataContratacao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public List<AulaColetiva> getAulas() {
        return aulas;
    }

    public List<FichaTreino> getFichasCriadas() {
        return fichasCriadas;
    }
}