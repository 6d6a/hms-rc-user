package ru.majordomo.hms.rc.user.validation.validator;

import com.google.i18n.phonenumbers.NumberParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.common.PhoneNumberManager;
import ru.majordomo.hms.rc.user.validation.ValidPhone;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public void initialize(ValidPhone validPhone) {
    }

    @Override
    public boolean isValid(final String phone, ConstraintValidatorContext constraintValidatorContext) {
        try {
            return PhoneNumberManager.phoneValid(phone);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
