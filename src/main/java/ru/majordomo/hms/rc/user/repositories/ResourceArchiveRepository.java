package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface ResourceArchiveRepository extends MongoRepository<ResourceArchive, String> {
    ResourceArchive findByResourceId(String resourceId);
    ResourceArchive findByIdAndAccountId(String resourceId, String accountId);
    List<ResourceArchive> findByAccountId(String accountId);
    List<ResourceArchive> findByServiceId(String serviceId);
    List<ResourceArchive> findByServiceIdAndAccountId(String serviceId, String accountId);
    Stream<ResourceArchive> findByCreatedAtBefore(LocalDateTime created);
}
