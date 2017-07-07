package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.rc.user.resources.Person;

public interface PersonRepository extends MongoRepository<Person,String> {
    List<Person> findByAccountId(String accountId);
    List<Person> findByLinkedAccountIds(String accountId);
    Person findByIdAndAccountId(String personId, String accountId);
    Person findByIdAndLinkedAccountIds(String personId, String accountId);
    @Query(value="{ 'nicHandle' : {$ne : ''} }", fields="{ 'nicHandle' : 1}")
    Stream<Person> findPersonsWithNicHandlesByNicHandleNotBlank();
}
