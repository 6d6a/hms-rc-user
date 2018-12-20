package ru.majordomo.hms.rc.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.model.Counter;

@Slf4j
@Service
public class CounterService {
    private MongoOperations mongoOperations;
    public static final String UNIX_ACCOUNT_UID_INTERNAL_NAME = "unixAccountUid";
    public static final int DEFAULT_START_UID = 70000;

    @Autowired
    public void setMongoOperations(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public Integer getNextUid() {
        Counter counter = mongoOperations.findAndModify(
                new Query(new Criteria("internalName").is(UNIX_ACCOUNT_UID_INTERNAL_NAME)),
                new Update().inc("count", 1),
                new FindAndModifyOptions().returnNew(true), //возвращает обновленный документ
                Counter.class
        );

        if (counter == null) {
            log.error(
                    "Counter with internalName '{}' not found, try to create new counter from max unixAccount's uid",
                    UNIX_ACCOUNT_UID_INTERNAL_NAME
            );
            counter = new Counter();
            counter.setCount(DEFAULT_START_UID);
            counter.setInternalName(UNIX_ACCOUNT_UID_INTERNAL_NAME);
            mongoOperations.insert(counter);
            return getNextUid();
        } else {
            log.debug("return next uid {}", counter.getCount());
            return counter.getCount();
        }
    }
}
