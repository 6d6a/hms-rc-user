package ru.majordomo.hms.rc.user.resources.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;



import ru.majordomo.hms.rc.user.resources.validation.validator.EmailOrDomainOrWildcardSubdomainValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailOrDomainOrWildcardSubdomainValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailOrDomainNameOrWildcardDomain {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidEmailOrDomainName.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}