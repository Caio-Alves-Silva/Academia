package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Recepcionista: cadastra planos, matrículas, aulas e registra entradas. */
@Entity
@Table(name = "recepcao")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Recepcao extends Usuario {

    @Column(name = "id_func", length = 20)
    private String idFunc;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Turno turno;

    @Column(length = 10)
    private String ramal;

    @Column(nullable = false)
    private Boolean ativo = Boolean.TRUE;

    protected Recepcao() {
    }

    public Recepcao(String nome, String cpf, String email, String senhaHash, LocalDateTime criadoEm) {
        super(nome, cpf, email, senhaHash, Perfil.RECEPCAO, criadoEm);
    }

    public String getIdFunc() {
        return idFunc;
    }

    public void setIdFunc(String idFunc) {
        this.idFunc = idFunc;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public String getRamal() {
        return ramal;
    }

    public void setRamal(String ramal) {
        this.ramal = ramal;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}