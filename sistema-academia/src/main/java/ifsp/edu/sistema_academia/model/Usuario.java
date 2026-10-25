package ifsp.edu.sistema_academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Usuário do sistema (herança JOINED): {@link Aluno}, {@link Recepcao} ou {@link Instrutor}. O {@link Perfil} é
 * gravado na tabela base e define o papel (role) no JWT.
 */
@Entity
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Somente dígitos (11); a coluna comporta também o formato com pontuação (14). */
    @Column(nullable = false, length = 14)
    private String cpf;

    /** Sempre em minúsculas. */
    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    protected Usuario(String nome, String cpf, String email, String senhaHash, Perfil perfil,
                      LocalDateTime criadoEm) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}