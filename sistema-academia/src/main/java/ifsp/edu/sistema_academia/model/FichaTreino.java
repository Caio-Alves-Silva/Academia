package ifsp.edu.sistema_academia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficha de treino de um aluno, composta por {@link ItemTreino}. Só uma ficha por aluno é a atual; as anteriores
 * formam o histórico.
 */
@Entity
@Table(name = "ficha_treino")
public class FichaTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    private Instrutor instrutor;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "is_atual", nullable = false)
    private Boolean isAtual = Boolean.TRUE;

    @Column(length = 500)
    private String observacoes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ficha_treino_id", nullable = false)
    @OrderColumn(name = "ordem")
    private List<ItemTreino> itens = new ArrayList<>();

    protected FichaTreino() {
    }

    public FichaTreino(Aluno aluno, Instrutor instrutor, LocalDateTime criadaEm, String observacoes,
                       List<ItemTreino> itens) {
        this.aluno = aluno;
        this.instrutor = instrutor;
        this.criadaEm = criadaEm;
        this.observacoes = observacoes;
        this.itens = new ArrayList<>(itens);
    }

    /** Move a ficha para o histórico ({@code isAtual = false}). */
    public void arquivar() {
        this.isAtual = Boolean.FALSE;
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Instrutor getInstrutor() {
        return instrutor;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public Boolean getIsAtual() {
        return isAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<ItemTreino> getItens() {
        return itens;
    }
}
