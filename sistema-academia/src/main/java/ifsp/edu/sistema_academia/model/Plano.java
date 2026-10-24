package ifsp.edu.sistema_academia.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** Plano: a oferta da academia (duração, valor e o que inclui). Não é o contrato do aluno — ver {@link Matricula}. */
@Entity
@Table(name = "planos")
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "duracao_meses", nullable = false)
    private int duracaoMeses;

    @Column(name = "valor_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plano_modalidades", joinColumns = @JoinColumn(name = "plano_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 30)
    private Set<Modalidade> modalidades = new HashSet<>();

    @Column(name = "limite_aulas_semana", nullable = false)
    private int limiteAulasSemana;

    protected Plano() {
    }

    public Plano(String nome, int duracaoMeses, BigDecimal valorMensal, Set<Modalidade> modalidades,
                 int limiteAulasSemana) {
        this.nome = nome;
        this.duracaoMeses = duracaoMeses;
        this.valorMensal = valorMensal;
        this.modalidades = new HashSet<>(modalidades);
        this.limiteAulasSemana = limiteAulasSemana;
    }

    public boolean incluiModalidade(Modalidade modalidade) {
        return modalidades.contains(modalidade);
    }

    /** RF07: cancelamento de planos de 3 ou 12 meses gera multa. */
    public boolean temMultaDeCancelamento() {
        return duracaoMeses >= 3;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getDuracaoMeses() {
        return duracaoMeses;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public Set<Modalidade> getModalidades() {
        return modalidades;
    }

    public int getLimiteAulasSemana() {
        return limiteAulasSemana;
    }
}
