package ru.majordomo.hms.rc.user.resources.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.rc.user.resources.validation.validator.LocalDatePastValidator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDatePastValidator.class)
@Documented
public @interface LocalDatePast {
    String message() default "ru.majordomo.hms.rc.user.resources.validation.LocalDatePast.message";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
