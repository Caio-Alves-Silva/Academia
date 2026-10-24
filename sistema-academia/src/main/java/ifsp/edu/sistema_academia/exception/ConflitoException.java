package ifsp.edu.sistema_academia.exception;

/** A operação conflita com o estado atual dos dados (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}