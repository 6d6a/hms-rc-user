package ru.majordomo.hms.rc.user.resources.validation.validator;

import com.google.common.net.InternetDomainName;
import org.apache.commons.validator.routines.DomainValidator;

import javax.validation.ConstraintValidatorContext;

public interface BaseEmailOrDomainValidator {
    default boolean isEmailValid(final String emailOrDomainName, ConstraintValidatorContext constraintValidatorContext) {
        Boolean validEmail;
        Boolean validDomainName = true;

        try {
            if (!DomainValidator.getInstance().isValid(emailOrDomainName)) validDomainName = false;

            InternetDomainName domain = InternetDomainName.from(emailOrDomainName);
            domain.publicSuffix();
        } catch (Exception e) {
            validDomainName = false;
        }
        org.apache.commons.validator.routines.EmailValidator validator =
                org.apache.commons.validator.routines.EmailValidator.getInstance(true, true); //allowLocal, allowTLD

        validEmail = validator.isValid(emailOrDomainName);
        return validDomainName || validEmail;
    }
}
