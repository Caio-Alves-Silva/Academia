package ifsp.edu.sistema_academia.exception;

/** A inscrição já foi cancelada anteriormente. */
public class InscricaoJaCanceladaException extends ConflitoException {

    public InscricaoJaCanceladaException(Long id) {
        super("A inscrição " + id + " já está cancelada");
    }
}