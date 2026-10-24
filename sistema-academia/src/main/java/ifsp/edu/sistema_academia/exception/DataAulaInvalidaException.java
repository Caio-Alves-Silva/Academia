package ifsp.edu.sistema_academia.exception;

/** A data informada não corresponde a uma ocorrência válida da aula para a operação. */
public class DataAulaInvalidaException extends RegraNegocioException {

    public DataAulaInvalidaException(String mensagem) {
        super(mensagem);
    }
}