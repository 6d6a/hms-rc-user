package ru.majordomo.hms.rc.user.resources.validation.validator;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.ValidDatabase;

public class DatabaseValidator implements ConstraintValidator<ValidDatabase, Database> {

    @Override
    public void initialize(ValidDatabase validBillingOperation) {
    }

    @Override
    public boolean isValid(final Database database, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;

        try {
            DBType dbType = database.getType();
            if (database.getDatabaseUsers() != null) {
                for (DatabaseUser databaseUser : database.getDatabaseUsers()) {
                    DBType userType = databaseUser.getType();
                    if (dbType != userType) {
                        isValid = false;
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate("Тип базы данных: " + dbType +
                                        ". Тип пользователя с ID " + databaseUser.getId() + ": " + userType +
                                        ". Типы должны совпадать")
                                .addConstraintViolation();
                    }
                }
            }
        } catch (RuntimeException e) {
            System.out.println(e.toString());
            isValid = false;
        }

        return isValid;
    }
}
