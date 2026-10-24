package ifsp.edu.sistema_academia.exception;

/** Uma regra de negócio impede a operação (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}