package ifsp.edu.sistema_academia.exception;

/** O cancelamento só é permitido até 2 horas antes da aula (RF10). */
public class PrazoCancelamentoExpiradoException extends RegraNegocioException {

    public PrazoCancelamentoExpiradoException() {
        super("O cancelamento só é permitido até 2 horas antes do início da aula");
    }
}