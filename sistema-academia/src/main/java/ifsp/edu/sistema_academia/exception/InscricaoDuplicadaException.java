package ifsp.edu.sistema_academia.exception;

/** O aluno já está inscrito (ou na lista de espera) desta aula nesta data. */
public class InscricaoDuplicadaException extends ConflitoException {

    public InscricaoDuplicadaException() {
        super("O aluno já está inscrito ou na lista de espera desta aula nesta data");
    }
}