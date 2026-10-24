package ifsp.edu.sistema_academia.exception;

/** O aluno atingiu o limite de aulas coletivas por semana do seu plano (RF09). */
public class LimiteSemanalAtingidoException extends RegraNegocioException {

    public LimiteSemanalAtingidoException(int limite) {
        super("Limite de " + limite + " aula(s) coletiva(s) por semana atingido");
    }
}