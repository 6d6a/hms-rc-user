package ru.majordomo.hms.rc.user.resources.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.common.PhoneNumberManager;
import ru.majordomo.hms.rc.user.resources.validation.ValidPhone;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public void initialize(ValidPhone validPhone) {
    }

    @Override
    public boolean isValid(final String phone, ConstraintValidatorContext constraintValidatorContext) {
        return PhoneNumberManager.phoneValid(phone);
    }
}
