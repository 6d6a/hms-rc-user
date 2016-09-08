package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.Domain;

public interface DomainRepository extends MongoRepository<Domain,String> {
}
