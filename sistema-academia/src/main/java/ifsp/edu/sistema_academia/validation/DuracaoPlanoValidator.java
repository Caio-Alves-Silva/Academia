package ifsp.edu.sistema_academia.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class DuracaoPlanoValidator implements ConstraintValidator<DuracaoPlanoValida, Integer> {

    private static final Set<Integer> DURACOES_PERMITIDAS = Set.of(1, 3, 12);

    @Override
    public boolean isValid(Integer valor, ConstraintValidatorContext context) {
        return valor == null || DURACOES_PERMITIDAS.contains(valor);
    }
}