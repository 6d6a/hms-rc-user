package ru.majordomo.hms.rc.user.resources.validation.validator;

import com.google.common.net.InternetDomainName;

import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.validation.DomainName;

@Component
public class DomainNameValidator implements ConstraintValidator<DomainName, String> {

    @Override
    public void initialize(DomainName domainName) {
    }

    @Override
    public boolean isValid(String domainName, ConstraintValidatorContext constraintValidatorContext) {
        try {
            InternetDomainName domain = InternetDomainName.from(domainName);
            String domainPublicPart = domain.publicSuffix().toString();
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
