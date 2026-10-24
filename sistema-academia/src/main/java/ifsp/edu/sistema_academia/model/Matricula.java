package ifsp.edu.sistema_academia.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Matrícula: contrato de um aluno com um plano, com início, fim e as mensalidades daquele período.
 */
@Entity
@Table(name = "matriculas")
public class Matricula {

    /** RF07: multa sobre o valor das mensalidades canceladas em planos de 3 ou 12 meses. */
    public static final BigDecimal PERCENTUAL_MULTA_CANCELAMENTO = new BigDecimal("0.10");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMatricula status = StatusMatricula.ATIVA;

    @Column(name = "data_encerramento")
    private LocalDate dataEncerramento;

    @Column(name = "multa_cancelamento", nullable = false, precision = 10, scale = 2)
    private BigDecimal multaCancelamento = BigDecimal.ZERO;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL)
    @OrderBy("numero ASC")
    private List<Mensalidade> mensalidades = new ArrayList<>();

    protected Matricula() {
    }

    /**
     * Cria a matrícula e gera todas as mensalidades da vigência (RF04): a primeira vence na data de início e as
     * demais no mesmo dia dos meses seguintes (ajustado ao último dia quando o mês é mais curto).
     *
     * @param descontoPrimeiraMensalidade crédito a abater da primeira parcela (limitado ao valor da parcela)
     */
    public static Matricula iniciar(Usuario aluno, Plano plano, LocalDate inicio,
                                    BigDecimal descontoPrimeiraMensalidade) {
        Matricula matricula = new Matricula();
        matricula.aluno = aluno;
        matricula.plano = plano;
        matricula.dataInicio = inicio;
        matricula.dataFim = inicio.plusMonths(plano.getDuracaoMeses()).minusDays(1);
        for (int i = 0; i < plano.getDuracaoMeses(); i++) {
            Mensalidade mensalidade = new Mensalidade(
                    matricula,
                    i + 1,
                    plano.getValorMensal(),
                    inicio.plusMonths(i),
                    inicio.plusMonths(i),
                    inicio.plusMonths(i + 1L).minusDays(1));
            if (i == 0 && descontoPrimeiraMensalidade != null) {
                mensalidade.aplicarDesconto(descontoPrimeiraMensalidade);
            }
            matricula.mensalidades.add(mensalidade);
        }
        return matricula;
    }

    /** Ativa significa: status ATIVA e a vigência ainda não terminou. */
    public boolean estaAtiva(LocalDate hoje) {
        return status == StatusMatricula.ATIVA && !hoje.isAfter(dataFim);
    }

    /** Status exibido: uma matrícula ATIVA cuja vigência terminou é apresentada como CONCLUIDA. */
    public StatusMatricula statusEfetivo(LocalDate hoje) {
        if (status == StatusMatricula.ATIVA && hoje.isAfter(dataFim)) {
            return StatusMatricula.CONCLUIDA;
        }
        return status;
    }

    /** Mensalidades pendentes com vencimento depois de hoje. */
    public List<Mensalidade> mensalidadesFuturas(LocalDate hoje) {
        return mensalidades.stream()
                .filter(m -> m.estaPendente() && m.getVencimento().isAfter(hoje))
                .toList();
    }

    /** Soma, sobre as parcelas pagas, do valor correspondente aos dias ainda não usados (RF06). */
    public BigDecimal creditoNaoUtilizado(LocalDate hoje) {
        return mensalidades.stream()
                .map(m -> m.valorNaoUtilizado(hoje))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** RF06: encerra o contrato por troca de plano e cancela as mensalidades futuras. */
    public List<Mensalidade> encerrarPorTroca(LocalDate hoje) {
        List<Mensalidade> canceladas = cancelarMensalidadesFuturas(hoje);
        this.status = StatusMatricula.ENCERRADA;
        this.dataEncerramento = hoje;
        return canceladas;
    }

    /**
     * RF07: cancela o contrato e as mensalidades futuras. Em planos de 3 ou 12 meses registra a multa de 10% sobre
     * o valor das mensalidades canceladas.
     */
    public List<Mensalidade> cancelar(LocalDate hoje) {
        List<Mensalidade> canceladas = cancelarMensalidadesFuturas(hoje);
        if (plano.temMultaDeCancelamento()) {
            this.multaCancelamento = valorTotal(canceladas)
                    .multiply(PERCENTUAL_MULTA_CANCELAMENTO)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        this.status = StatusMatricula.CANCELADA;
        this.dataEncerramento = hoje;
        return canceladas;
    }

    /** Soma dos valores líquidos das mensalidades informadas. */
    public static BigDecimal valorTotal(List<Mensalidade> mensalidades) {
        return mensalidades.stream()
                .map(Mensalidade::valorLiquido)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<Mensalidade> cancelarMensalidadesFuturas(LocalDate hoje) {
        List<Mensalidade> futuras = mensalidadesFuturas(hoje);
        futuras.forEach(Mensalidade::cancelar);
        return futuras;
    }

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public Plano getPlano() {
        return plano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    public LocalDate getDataEncerramento() {
        return dataEncerramento;
    }

    public BigDecimal getMultaCancelamento() {
        return multaCancelamento;
    }

    public List<Mensalidade> getMensalidades() {
        return mensalidades;
    }
}
