package ru.majordomo.hms.rc.user.validation;

import javax.validation.Constraint;
import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^(/(?!\\.)[\\.a-zA-Zа-яА-Я0-9ёЁ\\-_]*)+", message = "{ru.majordomo.hms.rc.user.validation.RelativeFilePath.message}")
public @interface ValidAbsoluteFilePath {
    String message() default "{ru.majordomo.hms.rc.user.validation.RelativeFilePath.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
