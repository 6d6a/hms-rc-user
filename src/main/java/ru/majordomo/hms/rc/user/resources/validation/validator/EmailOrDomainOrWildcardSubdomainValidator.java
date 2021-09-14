package ru.majordomo.hms.rc.user.resources.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.validation.ValidEmailOrDomainNameOrWildcardDomain;

public class EmailOrDomainOrWildcardSubdomainValidator implements ConstraintValidator<ValidEmailOrDomainNameOrWildcardDomain, String>, BaseEmailOrDomainValidator {
    @Override
    public void initialize(ValidEmailOrDomainNameOrWildcardDomain validEmailOrDomainName) {
    }

    @Override
    public boolean isValid(final String emailOrDomainNameOrWildcardDomain, ConstraintValidatorContext constraintValidatorContext) {

        return emailOrDomainNameOrWildcardDomain.startsWith("*.") ?
                this.isEmailValid(emailOrDomainNameOrWildcardDomain.substring(2),constraintValidatorContext) :
                this.isEmailValid(emailOrDomainNameOrWildcardDomain,constraintValidatorContext);
    }
}
