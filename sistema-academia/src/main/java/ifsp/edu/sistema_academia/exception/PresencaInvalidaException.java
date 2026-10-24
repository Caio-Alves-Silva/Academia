package ifsp.edu.sistema_academia.exception;

/** Lista de presença inconsistente com as inscrições da aula. */
public class PresencaInvalidaException extends RegraNegocioException {

    public PresencaInvalidaException(String mensagem) {
        super(mensagem);
    }
}