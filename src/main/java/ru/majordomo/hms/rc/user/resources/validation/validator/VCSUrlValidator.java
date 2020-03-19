package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.resources.validation.ValidAppLoadUrl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class VCSUrlValidator implements ConstraintValidator<ValidAppLoadUrl, String> {
    private UrlValidator urlValidator;

    @Override
    public void initialize(ValidAppLoadUrl validAppLoadUrl) {
        urlValidator = new UrlValidator(new String[] {"git+http", "git+https", "git+ssh"});
    }

    @Override
    public boolean isValid(String url, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isEmpty(url) || urlValidator.isValid(url);
    }
}
