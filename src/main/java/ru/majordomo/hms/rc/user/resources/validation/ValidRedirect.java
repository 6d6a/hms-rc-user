package ru.majordomo.hms.rc.user.resources.validation;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.majordomo.hms.rc.user.resources.validation.validator.RedirectValidator;

@Documented
@Constraint(validatedBy = RedirectValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRedirect {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidRedirect.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
