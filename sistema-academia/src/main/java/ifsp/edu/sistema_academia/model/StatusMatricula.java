package ifsp.edu.sistema_academia.model;

public enum StatusMatricula {
    /** Contrato em vigor (desde que a data de fim ainda não tenha passado). */
    ATIVA,
    /** Cancelada pela recepção (RF07). */
    CANCELADA,
    /** Encerrada por troca de plano (RF06) ou por término natural da vigência (calculado na leitura). */
    ENCERRADA
}