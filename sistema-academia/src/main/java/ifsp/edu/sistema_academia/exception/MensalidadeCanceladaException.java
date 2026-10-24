package ifsp.edu.sistema_academia.exception;

/** Mensalidades canceladas não podem ser pagas. */
public class MensalidadeCanceladaException extends RegraNegocioException {

    public MensalidadeCanceladaException(Long id) {
        super("A mensalidade " + id + " foi cancelada e não pode ser paga");
    }
}