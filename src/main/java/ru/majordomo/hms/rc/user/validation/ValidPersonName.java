package ru.majordomo.hms.rc.user.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "[a-zA-Z0-9а-яА-ЯёЁ \\.\\-]+", message = "{ru.majordomo.hms.rc.user.validation.PersonName.message}")

public @interface ValidPersonName {
    String message() default "{ru.majordomo.hms.rc.user.validation.PersonName.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
