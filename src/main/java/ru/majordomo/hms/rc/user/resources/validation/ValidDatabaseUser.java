package ru.majordomo.hms.rc.user.resources.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.validator.DatabaseUserValidator;

@Documented
@Constraint(validatedBy = DatabaseUserValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@UniqueNameResource(DatabaseUser.class)
public @interface ValidDatabaseUser {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidDatabaseUser.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
