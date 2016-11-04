package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

import ru.majordomo.hms.rc.user.resources.Person;

public interface PersonRepository extends MongoRepository<Person,String> {
    Collection<Person> findAllByAccountId(String accountId);
}
