package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.validation.ValidMailbox;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class MailboxValidator implements ConstraintValidator<ValidMailbox, Mailbox> {
    private final MongoOperations operations;

    @Autowired
    public MailboxValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidMailbox validMailbox) {
    }

    @Override
    public boolean isValid(final Mailbox mailbox, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;

            if (mailbox.getId() != null) {
                query = new Query(where("_id").nin(mailbox.getId()).and("name").is(mailbox.getName()).and("domainId").is(mailbox.getDomainId()));
            } else {
                query = new Query(where("name").is(mailbox.getName()).and("domainId").is(mailbox.getDomainId()));
            }

            isValid = !operations.exists(query, Mailbox.class);
        } catch (RuntimeException e) {
            return false;
        }
        return isValid;
    }
}
