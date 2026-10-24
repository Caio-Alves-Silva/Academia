package ifsp.edu.sistema_academia.exception;

/** O usuário autenticado não pode acessar o recurso (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}