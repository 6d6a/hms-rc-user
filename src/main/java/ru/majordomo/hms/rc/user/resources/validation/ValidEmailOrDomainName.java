package ru.majordomo.hms.rc.user.resources.validation;


import ru.majordomo.hms.rc.user.resources.validation.validator.EmailOrDomainValidator;
import ru.majordomo.hms.rc.user.resources.validation.validator.EmailValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailOrDomainValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailOrDomainName {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidEmailOrDomainName.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
