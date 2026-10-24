package ifsp.edu.sistema_academia.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A duração de um plano só pode ser de 1, 3 ou 12 meses (RF03). Valores nulos são ignorados (use @NotNull). */
@Documented
@Constraint(validatedBy = DuracaoPlanoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DuracaoPlanoValida {

    String message() default "A duração deve ser de 1, 3 ou 12 meses";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}