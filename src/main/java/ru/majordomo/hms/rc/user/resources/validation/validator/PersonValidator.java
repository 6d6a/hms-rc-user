package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.validation.ValidPerson;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class PersonValidator implements ConstraintValidator<ValidPerson, Person> {
    private final MongoOperations operations;

    @Autowired
    public PersonValidator(
            MongoOperations operations
    ) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidPerson validPerson) {
    }

    @Override
    public boolean isValid(final Person person, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = false;
//        try {
//            Query query;
//
//            if (webSite.getId() != null) {
//                query = new Query(where("_id").nin(webSite.getId()).and("domainIds").in(webSite.getDomainIds()));
//            } else {
//                query = new Query(where("domainIds").in(webSite.getDomainIds()));
//            }
//
//            isValid = !operations.exists(query, WebSite.class);
//        } catch (RuntimeException e) {
//            return false;
//        }

        return isValid;
    }
}
