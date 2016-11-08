package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.DatabaseUser;

import java.util.List;

public interface DatabaseUserRepository extends MongoRepository<DatabaseUser, String> {
    List<DatabaseUser> findByAccountId(String accountId);
}
