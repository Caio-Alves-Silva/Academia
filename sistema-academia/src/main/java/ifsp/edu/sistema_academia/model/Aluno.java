package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Aluno da academia. */
@Entity
@Table(name = "aluno")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Aluno extends Usuario {

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String telefone;

    @Column(length = 255)
    private String objetivo;

    @Column(name = "restricoes_medicas", length = 500)
    private String restricoesMedicas;

    @OneToMany(mappedBy = "aluno")
    private List<Matricula> matriculas = new ArrayList<>();

    @OneToMany(mappedBy = "aluno")
    private List<Inscricao> inscricoes = new ArrayList<>();

    @OneToMany(mappedBy = "aluno")
    private List<FichaTreino> fichasTreino = new ArrayList<>();

    @OneToMany(mappedBy = "aluno")
    private List<Checkin> checkins = new ArrayList<>();

    protected Aluno() {
    }

    public Aluno(String nome, String cpf, String email, String senhaHash, LocalDateTime criadoEm) {
        super(nome, cpf, email, senhaHash, Perfil.ALUNO, criadoEm);
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(String objetivo) {
        this.objetivo = objetivo;
    }

    public String getRestricoesMedicas() {
        return restricoesMedicas;
    }

    public void setRestricoesMedicas(String restricoesMedicas) {
        this.restricoesMedicas = restricoesMedicas;
    }

    public List<Matricula> getMatriculas() {
        return matriculas;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }

    public List<FichaTreino> getFichasTreino() {
        return fichasTreino;
    }

    public List<Checkin> getCheckins() {
        return checkins;
    }
}