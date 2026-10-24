package ifsp.edu.sistema_academia.model;

import java.util.List;

public enum StatusInscricao {
    CONFIRMADA,
    LISTA_ESPERA,
    CANCELADA;

    /** Status que ocupam vaga ou posição na fila (contam para o limite semanal). */
    public static final List<StatusInscricao> VIGENTES = List.of(CONFIRMADA, LISTA_ESPERA);
}
