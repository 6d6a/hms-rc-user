package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.validation.UniqueNameResource;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class UniqueNameResourceValidator implements ConstraintValidator<UniqueNameResource, Resource> {
    private final MongoOperations operations;
    private Class<? extends Resource> objectModel;

    @Autowired
    public UniqueNameResourceValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(UniqueNameResource uniqueNameResource) {
        this.objectModel = uniqueNameResource.value();
    }

    @Override
    public boolean isValid(final Resource resource, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;

            if (resource.getId() != null) {
                query = new Query(where("id").nin(resource.getId()).and("name").is(resource.getName()));
            } else {
                query = new Query(where("name").is(resource.getName()));
            }

            isValid = !operations.exists(query, this.objectModel);
        } catch (RuntimeException e) {
            return false;
        }
        return isValid;
    }
}
