package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.ValidDatabaseUser;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class DatabaseUserValidator implements ConstraintValidator<ValidDatabaseUser, DatabaseUser> {
    private final MongoOperations operations;

    @Autowired
    public DatabaseUserValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidDatabaseUser validDatabaseUser) {
    }

    @Override
    public boolean isValid(final DatabaseUser databaseUser, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;

        try {
            if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
                for (String databaseId : databaseUser.getDatabaseIds()) {
                    Query query = new Query(where("_id").is(databaseId).and("accountId").is(databaseUser.getAccountId()));

                    if (!operations.exists(query, Database.class)) {
                        isValid = false;
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate("Не найдена база данных с ID: " + databaseId)
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
