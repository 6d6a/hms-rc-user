package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.Passport;

public interface PassportRepository extends MongoRepository<Passport,String> {
}
