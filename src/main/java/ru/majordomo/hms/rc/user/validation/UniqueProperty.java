package ru.majordomo.hms.rc.user.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.validation.validator.UniquePropertyValidator;

@Documented
@Constraint(validatedBy = UniquePropertyValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueProperty {
    String message() default "{ru.majordomo.hms.rc.user.validation.UniqueProperty.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends Resource> value();

    String collection() default "";

    String fieldName();

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        UniqueProperty[] value();
    }
}
