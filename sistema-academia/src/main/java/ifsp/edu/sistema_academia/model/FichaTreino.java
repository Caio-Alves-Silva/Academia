package ifsp.edu.sistema_academia.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ficha de treino de um aluno. Só uma ficha por aluno é a atual; as anteriores formam o histórico. */
@Entity
@Table(name = "fichas_treino")
public class FichaTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    private Usuario instrutor;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    private boolean atual = true;

    @Column(length = 500)
    private String observacoes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ficha_itens", joinColumns = @JoinColumn(name = "ficha_id"))
    @OrderColumn(name = "ordem")
    private List<ItemTreino> itens = new ArrayList<>();

    protected FichaTreino() {
    }

    public FichaTreino(Usuario aluno, Usuario instrutor, LocalDateTime criadaEm, String observacoes,
                       List<ItemTreino> itens) {
        this.aluno = aluno;
        this.instrutor = instrutor;
        this.criadaEm = criadaEm;
        this.observacoes = observacoes;
        this.itens = new ArrayList<>(itens);
    }

    /** Move a ficha para o histórico. */
    public void arquivar() {
        this.atual = false;
    }

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public Usuario getInstrutor() {
        return instrutor;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public boolean isAtual() {
        return atual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<ItemTreino> getItens() {
        return itens;
    }
}
