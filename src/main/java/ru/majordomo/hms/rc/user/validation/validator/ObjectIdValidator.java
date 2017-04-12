package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.validation.ObjectId;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class ObjectIdValidator implements ConstraintValidator<ObjectId, String> {
    private final MongoOperations operations;
    private Class<? extends Resource> objectModel;
    private String collection;
    private String fieldName;

    @Autowired
    public ObjectIdValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectId objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
        this.fieldName = objectId.fieldName();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (s == null || s.equals("")) {
                return true;
            } else {
                boolean foundObject;
                Query query;
                if (fieldName.equals("")) {
                    query = new Query(where("_id").is(s));
                } else {
                    query = new Query(where(fieldName).is(s));
                }
                if (collection.equals("")) {
                    foundObject = operations.exists(query, this.objectModel);
                } else {
                    foundObject = operations.exists(query, this.objectModel, collection);
                }
                return foundObject;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}
