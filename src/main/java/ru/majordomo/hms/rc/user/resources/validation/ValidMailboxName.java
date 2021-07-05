package ru.majordomo.hms.rc.user.resources.validation;

import ru.majordomo.hms.rc.user.resources.validation.validator.MailboxNameValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MailboxNameValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
//@Pattern(regexp = "[a-z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+=?^_`{|}~-]+)*", message = "{ru.majordomo.hms.rc.user.resources.validation.MailboxName.message}")
public @interface ValidMailboxName {
    String message() default "{ru.majordomo.hms.rc.user.resources.validation.MailboxName.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
