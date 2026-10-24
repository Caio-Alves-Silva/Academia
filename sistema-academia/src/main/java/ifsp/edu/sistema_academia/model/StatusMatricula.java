package ifsp.edu.sistema_academia.model;

public enum StatusMatricula {
    /** Contrato em vigor (desde que a data de fim ainda não tenha passado). */
    ATIVA,
    /** Vigência terminada naturalmente. Calculado na leitura quando a data de fim passou. */
    CONCLUIDA,
    /** Cancelada pela recepção (RF07). */
    CANCELADA,
    /** Encerrada por troca de plano (RF06). */
    ENCERRADA
}
