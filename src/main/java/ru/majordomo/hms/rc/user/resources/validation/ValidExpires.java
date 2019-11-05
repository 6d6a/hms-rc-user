package ru.majordomo.hms.rc.user.resources.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^(max|-?\\d?\\d[mhd]?)?$", message = "{ru.majordomo.hms.rc.user.resources.validation.ValidExpires.message}")
public @interface ValidExpires {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.ValidExpires.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
