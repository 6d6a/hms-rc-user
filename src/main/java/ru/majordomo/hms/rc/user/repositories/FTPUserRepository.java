package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.FTPUser;

public interface FTPUserRepository extends MongoRepository<FTPUser, String> {
}
