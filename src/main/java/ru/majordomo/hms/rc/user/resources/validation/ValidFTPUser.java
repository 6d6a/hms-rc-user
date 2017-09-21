package ru.majordomo.hms.rc.user.resources.validation;

import ru.majordomo.hms.rc.user.resources.validation.validator.FTPUserValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FTPUserValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFTPUser {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidFTPUser.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
