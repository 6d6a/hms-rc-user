package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.DatabaseUser;

public interface DatabaseUserRepository extends MongoRepository<DatabaseUser, String> {

}
