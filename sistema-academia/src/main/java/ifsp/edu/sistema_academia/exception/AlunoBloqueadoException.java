package ifsp.edu.sistema_academia.exception;

/** Aluno com mensalidade vencida há mais de 5 dias (RF05): não entra na academia nem se inscreve em aulas. */
public class AlunoBloqueadoException extends RegraNegocioException {

    public AlunoBloqueadoException() {
        super("Aluno bloqueado: há mensalidade vencida há mais de 5 dias");
    }
}