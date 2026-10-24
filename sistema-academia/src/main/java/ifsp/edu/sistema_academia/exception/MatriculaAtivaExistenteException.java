package ifsp.edu.sistema_academia.exception;

/** O aluno já possui uma matrícula ativa (RF04). */
public class MatriculaAtivaExistenteException extends ConflitoException {

    public MatriculaAtivaExistenteException(Long alunoId) {
        super("O aluno " + alunoId + " já possui uma matrícula ativa");
    }
}