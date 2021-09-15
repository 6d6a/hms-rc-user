package ru.majordomo.hms.rc.user.resources.validation.validator;

import ru.majordomo.hms.rc.user.resources.validation.ValidEmailOrDomainName;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailOrDomainValidator implements ConstraintValidator<ValidEmailOrDomainName, String>, BaseEmailOrDomainValidator {
    @Override
    public void initialize(ValidEmailOrDomainName validEmailOrDomainName) {
    }

    @Override
    public boolean isValid(final String emailOrDomainName, ConstraintValidatorContext constraintValidatorContext) {
        return this.isEmailValid(emailOrDomainName,constraintValidatorContext);
    }
}
