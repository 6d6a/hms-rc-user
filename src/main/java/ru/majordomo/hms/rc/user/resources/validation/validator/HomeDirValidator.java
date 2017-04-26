package ru.majordomo.hms.rc.user.resources.validation.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.validation.ValidHomeDir;

public class HomeDirValidator implements ConstraintValidator<ValidHomeDir, String> {
    @Override
    public void initialize(ValidHomeDir validHomeDir) {
    }

    @Override
    public boolean isValid(final String homeDir, ConstraintValidatorContext constraintValidatorContext) {
        Pattern p = Pattern.compile("^/home$|^/home/$|^/$");
        Matcher m = p.matcher(homeDir);
        return !m.matches();
    }
}
