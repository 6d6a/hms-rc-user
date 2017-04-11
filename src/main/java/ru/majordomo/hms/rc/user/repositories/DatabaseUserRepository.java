package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.DatabaseUser;

import java.util.List;

public interface DatabaseUserRepository extends MongoRepository<DatabaseUser, String> {
    List<DatabaseUser> findByAccountId(String accountId);
    DatabaseUser findByIdAndAccountId(String databaseUserId, String accountId);
    DatabaseUser findByName(String name);
    List<DatabaseUser> findByServiceId(String serviceId);
    List<DatabaseUser> findByServiceIdAndAccountId(String serviceId, String accountId);
}
