package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

import ru.majordomo.hms.rc.user.resources.Person;

public interface PersonRepository extends MongoRepository<Person,String> {
    List<Person> findByAccountId(String accountId);
}
