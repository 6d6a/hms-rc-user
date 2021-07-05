package ru.majordomo.hms.rc.user.resources.validation.validator;

import lombok.NoArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import ru.majordomo.hms.rc.user.resources.validation.ValidMailboxName;

import javax.annotation.Nullable;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * убраны все спецсимволы так как по факту на нашей почте они не работают.
 * Проверялось методом тыка.
 * Старый {@code "^[a-z0-9!#$%&'*+=?^_`{|}~-]+$"}
 */
@NoArgsConstructor
public class MailboxNameValidator implements ConstraintValidator<ValidMailboxName, String>  {

    private static final Pattern HELP_PATTERN = Pattern.compile("^[.a-z0-9_-]+$");
    private static final Set<String> RESERVED_NAMES = Collections.singleton("*");

    private final EmailValidatorWrapper emailValidator = new EmailValidatorWrapper();

    @Override
    public boolean isValid(@Nullable String mailboxName, @Nullable ConstraintValidatorContext context) {
        if (mailboxName == null || !emailValidator.isValidUser(mailboxName) || RESERVED_NAMES.contains(mailboxName)) {
            return false;
        }
        return HELP_PATTERN.matcher(mailboxName).matches();
    }

    public boolean isValid(@Nullable String mailboxName) {
        return isValid(mailboxName, null);
    }

    private static class EmailValidatorWrapper extends EmailValidator {
        public EmailValidatorWrapper() {
            super(false, false);
        }

        @Override
        public boolean isValidUser(String user) {
            return super.isValidUser(user);
        }
    }
}
