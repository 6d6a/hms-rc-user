package ru.majordomo.hms.rc.user.resources.validation;

import ru.majordomo.hms.rc.user.resources.validation.validator.VCSUrlValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = VCSUrlValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAppLoadUrl {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidAppLoadUrl.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
