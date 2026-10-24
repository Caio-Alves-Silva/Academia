package ifsp.edu.sistema_academia.exception;

import ifsp.edu.sistema_academia.model.Modalidade;

/** A modalidade da aula não faz parte do plano do aluno (RF09). */
public class ModalidadeNaoIncluidaException extends RegraNegocioException {

    public ModalidadeNaoIncluidaException(Modalidade modalidade) {
        super("A modalidade " + modalidade + " não está incluída no plano do aluno");
    }
}