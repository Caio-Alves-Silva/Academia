package ifsp.edu.sistema_academia.exception;

/** CPF ou e-mail já cadastrado. */
public class DadoDuplicadoException extends ConflitoException {

    public DadoDuplicadoException(String campo) {
        super(campo + " já cadastrado(a)");
    }
}