package ifsp.edu.sistema_academia.exception;

/** A mensalidade já foi paga (RF05). */
public class MensalidadeJaPagaException extends ConflitoException {

    public MensalidadeJaPagaException(Long id) {
        super("A mensalidade " + id + " já foi paga");
    }
}