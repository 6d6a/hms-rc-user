package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Optional;

public interface  OperationOversightRepository<T extends Resource> extends MongoRepository<OperationOversight, String> {
    Optional<OperationOversight<T>> findByResourceId(String resourceId);

    @Query(value="{'_id' : ?0}", fields="{}")
    Optional<OperationOversight<T>> findByOvsId(String id);
}
