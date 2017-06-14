package ru.majordomo.hms.rc.user.resources.validation.validator;


import java.time.LocalDate;
import java.time.temporal.Temporal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.validation.LocalDatePast;

public class LocalDatePastValidator implements ConstraintValidator<LocalDatePast, Temporal> {
    @Override
    public void initialize(LocalDatePast constraintAnnotation) {
    }

    @Override
    public boolean isValid(Temporal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate ld = LocalDate.from(value);
        return ld.isBefore(LocalDate.now());
    }
}
