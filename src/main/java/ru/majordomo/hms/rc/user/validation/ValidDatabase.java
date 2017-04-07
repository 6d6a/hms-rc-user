package ru.majordomo.hms.rc.user.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.validation.validator.DatabaseValidator;

@Documented
@Constraint(validatedBy = DatabaseValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@UniqueNameResource(Database.class)
public @interface ValidDatabase {
    String message() default "{ru.majordomo.hms.rc.user.validation.ValidDatabase.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
