package ru.majordomo.hms.rc.user.resources.validation;

import org.hibernate.validator.constraints.Length;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseChecks;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "[a-zA-Z0-9_]+", message = "{ru.majordomo.hms.rc.user.resources.validation.DatabaseName.message}")
@Length(max=64, message = "Имя не может быть длиннее 64 символов")
public @interface ValidDatabaseName {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.DatabaseName.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
