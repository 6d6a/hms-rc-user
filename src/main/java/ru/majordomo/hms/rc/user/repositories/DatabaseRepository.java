package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.rc.user.resources.Database;

public interface DatabaseRepository extends MongoRepository<Database, String> {
    List<Database> findAll();
    List<Database> findByAccountId(String accountId);
    Database findByIdAndAccountId(String databaseId, String accountId);
    Long countByAccountId(String accountId);
}
