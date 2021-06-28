package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.rc.user.resources.DKIM;
import ru.majordomo.hms.rc.user.resources.DTO.EntityIdOnly;

import javax.annotation.Nullable;
import java.util.Collection;

public interface DKIMRepository extends MongoRepository<DKIM, String> {
    @Nullable
    @Query(fields = "{ 'publicKey': 1, 'selector': 1, 'switchedOn': 1 }", value = "{ '_id': ?0 }")
    DKIM findWithoutPrivateKey(String id);

    @Nullable
    @Query(fields = "{ 'privateKey': 1 }", value = "{ '_id': ?0 }")
    DKIM findPrivateKeyOnly(String id);

    default void setSwitchedOn(String id, boolean switchedOn) {
        findById(id).ifPresent(dkim -> {
            dkim.setSwitchedOn(switchedOn);
            save(dkim);
        });
    }

    Collection<EntityIdOnly> findAllBy();
}
