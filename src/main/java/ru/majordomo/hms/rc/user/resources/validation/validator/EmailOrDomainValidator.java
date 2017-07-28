package ru.majordomo.hms.rc.user.resources.validation.validator;

import com.google.common.net.InternetDomainName;
import org.apache.commons.validator.routines.DomainValidator;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmailOrDomainName;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailOrDomainValidator implements ConstraintValidator<ValidEmailOrDomainName, String> {
    @Override
    public void initialize(ValidEmailOrDomainName validEmailOrDomainName) {
    }

    @Override
    public boolean isValid(final String emailOrDomainName, ConstraintValidatorContext constraintValidatorContext) {
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
