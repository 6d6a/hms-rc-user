package ru.majordomo.hms.rc.user.resources.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.validation.validator.DnsRecordValidator;

@Documented
@Constraint(validatedBy = DnsRecordValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
//@UniqueNameResource(Database.class)
public @interface ValidDnsRecord {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidDnsRecord.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "";
}
