package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.validation.ObjectIdCollection;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class ObjectIdCollectionValidator implements ConstraintValidator<ObjectIdCollection, Collection<String>> {
    private final MongoTemplate mongoTemplate;
    private Class<? extends Resource> objectModel;
    private String collection;

    @Autowired
    public ObjectIdCollectionValidator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void initialize(ObjectIdCollection objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
    }

    @Override
    public boolean isValid(Collection<String> items, ConstraintValidatorContext constraintValidatorContext) {
        if (items == null || items.isEmpty()) {
            return true;
        } else {
            for (String next : items) {
                try {
                    boolean foundObject;
                    if (!collection.equals("")) {
                        foundObject = mongoTemplate.exists(
                                new Query(where("_id").is(next)),
                                this.objectModel,
                                collection
                        );
                    } else {
                        foundObject = mongoTemplate.exists(
                                new Query(where("_id").is(next)),
                                this.objectModel
                        );
                    }

                    if (!foundObject) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }
}
