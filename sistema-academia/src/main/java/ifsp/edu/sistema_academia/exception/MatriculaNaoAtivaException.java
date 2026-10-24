package ifsp.edu.sistema_academia.exception;

/** O aluno não possui matrícula ativa (ou a matrícula informada não está ativa). */
public class MatriculaNaoAtivaException extends RegraNegocioException {

    public MatriculaNaoAtivaException(String mensagem) {
        super(mensagem);
    }

    public static MatriculaNaoAtivaException doAluno(Long alunoId) {
        return new MatriculaNaoAtivaException("O aluno " + alunoId + " não possui matrícula ativa");
    }
}