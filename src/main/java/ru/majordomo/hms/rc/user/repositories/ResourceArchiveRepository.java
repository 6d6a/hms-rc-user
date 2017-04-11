package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

import java.util.List;

public interface ResourceArchiveRepository extends MongoRepository<ResourceArchive, String> {
    ResourceArchive findByResourceId(String resourceId);
    ResourceArchive findByIdAndAccountId(String resourceId, String accountId);
    List<ResourceArchive> findByAccountId(String accountId);
    List<ResourceArchive> findByServiceId(String serviceId);
    List<ResourceArchive> findByServiceIdAndAccountId(String serviceId, String accountId);
}
