package ifsp.edu.sistema_academia.exception;

/** O instrutor já tem uma aula que se sobrepõe ao horário informado (RF08). */
public class ConflitoHorarioInstrutorException extends ConflitoException {

    public ConflitoHorarioInstrutorException(String instrutor, Long aulaConflitanteId) {
        super("O instrutor " + instrutor + " já tem a aula " + aulaConflitanteId
                + " em horário sobreposto");
    }
}