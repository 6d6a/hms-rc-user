package ru.majordomo.hms.rc.user.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.validation.ValidEmail;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    @Override
    public void initialize(ValidEmail validEmail) {
    }

    @Override
    public boolean isValid(final String email, ConstraintValidatorContext constraintValidatorContext) {
        org.apache.commons.validator.routines.EmailValidator validator =
                org.apache.commons.validator.routines.EmailValidator.getInstance(true, true); //allowLocal, allowTLD
        return validator.isValid(email);
    }
}
