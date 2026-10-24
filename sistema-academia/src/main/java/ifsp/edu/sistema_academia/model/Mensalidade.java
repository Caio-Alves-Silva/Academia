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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Parcela de uma {@link Matricula}. O vencimento cai no mesmo dia de cada mês. */
@Entity
@Table(name = "mensalidades")
public class Mensalidade {

    /** RF05: multa por pagamento após o vencimento. */
    public static final BigDecimal PERCENTUAL_MULTA_ATRASO = new BigDecimal("0.02");

    /** RF05: dias de atraso tolerados; acima disso o aluno fica bloqueado. */
    public static final int DIAS_ATE_BLOQUEIO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    /** Número da parcela dentro da matrícula (1..n). */
    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Desconto aplicado (ex.: crédito proporcional de uma troca de plano). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate vencimento;

    /** Primeiro dia coberto por esta parcela. */
    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    /** Último dia coberto por esta parcela. */
    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMensalidade status = StatusMensalidade.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal multa = BigDecimal.ZERO;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago;

    protected Mensalidade() {
    }

    public Mensalidade(Matricula matricula, int numero, BigDecimal valor, LocalDate vencimento,
                       LocalDate periodoInicio, LocalDate periodoFim) {
        this.matricula = matricula;
        this.numero = numero;
        this.valor = valor;
        this.vencimento = vencimento;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
    }

    /** Valor da parcela já descontado. */
    public BigDecimal valorLiquido() {
        return valor.subtract(desconto);
    }

    public boolean estaPendente() {
        return status == StatusMensalidade.PENDENTE;
    }

    /** Pendente e com vencimento anterior a {@code hoje}. */
    public boolean estaVencida(LocalDate hoje) {
        return estaPendente() && vencimento.isBefore(hoje);
    }

    /** Vencida há mais de {@link #DIAS_ATE_BLOQUEIO} dias: bloqueia o aluno. */
    public boolean bloqueiaAluno(LocalDate hoje) {
        return estaPendente() && vencimento.isBefore(hoje.minusDays(DIAS_ATE_BLOQUEIO));
    }

    public long diasEmAtraso(LocalDate hoje) {
        return estaVencida(hoje) ? ChronoUnit.DAYS.between(vencimento, hoje) : 0;
    }

    /** Multa de 2% sobre o valor líquido se (e somente se) o pagamento ocorrer após o vencimento. */
    public BigDecimal calcularMulta(LocalDate hoje) {
        if (!estaVencida(hoje)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return valorLiquido().multiply(PERCENTUAL_MULTA_ATRASO).setScale(2, RoundingMode.HALF_UP);
    }

    /** Valor total a pagar se o pagamento fosse feito em {@code hoje}. */
    public BigDecimal valorAPagar(LocalDate hoje) {
        return valorLiquido().add(calcularMulta(hoje));
    }

    public void pagar(LocalDate hoje) {
        if (!estaPendente()) {
            throw new IllegalStateException("Somente mensalidades pendentes podem ser pagas");
        }
        this.multa = calcularMulta(hoje);
        this.valorPago = valorLiquido().add(multa);
        this.dataPagamento = hoje;
        this.status = StatusMensalidade.PAGA;
    }

    public void cancelar() {
        if (!estaPendente()) {
            throw new IllegalStateException("Somente mensalidades pendentes podem ser canceladas");
        }
        this.status = StatusMensalidade.CANCELADA;
    }

    public void aplicarDesconto(BigDecimal desconto) {
        this.desconto = desconto.min(valor).max(BigDecimal.ZERO);
    }

    /**
     * Parte do valor pago que corresponde a dias ainda não usados depois de {@code hoje}
     * (o dia de hoje conta como usado). Zero se a parcela não foi paga.
     */
    public BigDecimal valorNaoUtilizado(LocalDate hoje) {
        if (status != StatusMensalidade.PAGA) {
            return BigDecimal.ZERO.setScale(2);
        }
        LocalDate primeiroDiaNaoUsado = periodoInicio.isAfter(hoje) ? periodoInicio : hoje.plusDays(1);
        if (primeiroDiaNaoUsado.isAfter(periodoFim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long diasNaoUsados = ChronoUnit.DAYS.between(primeiroDiaNaoUsado, periodoFim) + 1;
        long diasDoPeriodo = ChronoUnit.DAYS.between(periodoInicio, periodoFim) + 1;
        return valorLiquido()
                .multiply(BigDecimal.valueOf(diasNaoUsados))
                .divide(BigDecimal.valueOf(diasDoPeriodo), 2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public int getNumero() {
        return numero;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public StatusMensalidade getStatus() {
        return status;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public BigDecimal getMulta() {
        return multa;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }
}
