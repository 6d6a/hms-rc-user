package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.validation.UniqueProperty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class UniquePropertyValidator implements ConstraintValidator<UniqueProperty, String> {
    private final MongoOperations operations;
    private Class<? extends Resource> objectModel;
    private String collection;
    private String fieldName;

    @Autowired
    public UniquePropertyValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(UniqueProperty objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
        this.fieldName = objectId.fieldName();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query = new Query(where(fieldName).is(s));
            if (collection.equals("")) {
                isValid = !operations.exists(query, this.objectModel);
            } else {
                isValid = !operations.exists(query, this.objectModel, collection);
            }
        } catch (RuntimeException e) {
            return false;
        }
        return isValid;
    }
}
