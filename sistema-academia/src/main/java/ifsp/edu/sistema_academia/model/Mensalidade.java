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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Parcela de uma {@link Matricula}. O vencimento cai no mesmo dia de cada mês. */
@Entity
@Table(name = "mensalidade")
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

    /** Valor devido da parcela (já com eventual desconto de troca de plano, sem multa). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    /** Primeiro dia coberto por esta parcela. */
    @Column(name = "periodo_inicial", nullable = false)
    private LocalDate periodoInicial;

    /** Último dia coberto por esta parcela. */
    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago;

    protected Mensalidade() {
    }

    public Mensalidade(Matricula matricula, int numero, BigDecimal valor, LocalDate vencimento,
                       LocalDate periodoInicial, LocalDate periodoFim) {
        this.matricula = matricula;
        this.numero = numero;
        this.valor = valor;
        this.vencimento = vencimento;
        this.periodoInicial = periodoInicial;
        this.periodoFim = periodoFim;
    }

    public boolean estaPendente() {
        return statusPagamento == StatusPagamento.PENDENTE;
    }

    /** Pendente e com vencimento anterior a {@code hoje}. */
    public boolean estaVencida(LocalDate hoje) {
        return estaPendente() && vencimento.isBefore(hoje);
    }

    /** Vencida há mais de {@link #DIAS_ATE_BLOQUEIO} dias: bloqueia o aluno. */
    public boolean bloqueiaAluno(LocalDate hoje) {
        return estaPendente() && vencimento.plusDays(DIAS_ATE_BLOQUEIO).isBefore(hoje);
    }

    public long diasEmAtraso(LocalDate hoje) {
        return estaVencida(hoje) ? ChronoUnit.DAYS.between(vencimento, hoje) : 0;
    }

    /** Multa fixa de 2% sobre o valor se (e somente se) o pagamento ocorrer após o vencimento. */
    public BigDecimal calcularMulta(LocalDate hoje) {
        if (!estaVencida(hoje)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return valor.multiply(PERCENTUAL_MULTA_ATRASO).setScale(2, RoundingMode.HALF_UP);
    }

    /** Valor total a pagar se o pagamento fosse feito em {@code hoje} (valor * 1,02 após o vencimento). */
    public BigDecimal valorAPagar(LocalDate hoje) {
        return valor.add(calcularMulta(hoje)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Multa efetivamente cobrada no pagamento (zero enquanto não paga ou se paga em dia). */
    public BigDecimal multaPaga() {
        if (valorPago == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return valorPago.subtract(valor);
    }

    public void pagar(LocalDate hoje) {
        if (!estaPendente()) {
            throw new IllegalStateException("Somente mensalidades pendentes podem ser pagas");
        }
        this.valorPago = valorAPagar(hoje);
        this.dataPagamento = hoje;
        this.statusPagamento = StatusPagamento.PAGO;
    }

    public void cancelar() {
        if (!estaPendente()) {
            throw new IllegalStateException("Somente mensalidades pendentes podem ser canceladas");
        }
        this.statusPagamento = StatusPagamento.CANCELADO;
    }

    /** Abate o desconto do valor da parcela (limitado ao valor da própria parcela). */
    public void aplicarDesconto(BigDecimal desconto) {
        BigDecimal aplicado = desconto.min(valor).max(BigDecimal.ZERO);
        this.valor = valor.subtract(aplicado);
    }

    /**
     * RF06: saldo pro-rata da parcela paga que ainda não foi usado: {@code (valor / diasDoMes) * diasRestantes},
     * onde {@code diasDoMes} é o tamanho do mês corrente e {@code diasRestantes} são os dias após hoje até o fim do
     * período coberto (hoje conta como usado). Parcela paga de período futuro volta inteira. Zero se não foi paga.
     */
    public BigDecimal valorNaoUtilizado(LocalDate hoje) {
        if (statusPagamento != StatusPagamento.PAGO) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (periodoInicial.isAfter(hoje)) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        }
        if (!hoje.isBefore(periodoFim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long diasRestantes = ChronoUnit.DAYS.between(hoje, periodoFim);
        return valor
                .divide(BigDecimal.valueOf(hoje.lengthOfMonth()), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(diasRestantes))
                .min(valor)
                .setScale(2, RoundingMode.HALF_UP);
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

    public LocalDate getVencimento() {
        return vencimento;
    }

    public LocalDate getPeriodoInicial() {
        return periodoInicial;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public StatusPagamento getStatusPagamento() {
        return statusPagamento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }
}
