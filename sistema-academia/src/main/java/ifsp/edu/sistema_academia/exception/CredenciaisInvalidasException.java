package ifsp.edu.sistema_academia.exception;

/** E-mail ou senha incorretos. A mensagem nunca revela qual dos dois campos está errado (HTTP 401). */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}