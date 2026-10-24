package ifsp.edu.sistema_academia.exception;

/** O aluno teve 2 ou mais faltas na semana anterior e não pode se inscrever nesta semana (RF11). */
public class BloqueioPorFaltasException extends RegraNegocioException {

    public BloqueioPorFaltasException(long faltas) {
        super("Inscrição bloqueada: " + faltas + " faltas na semana anterior");
    }
}