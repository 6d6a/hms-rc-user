package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.Domain;

import java.util.List;

public interface DomainRepository extends MongoRepository<Domain,String> {
    List<Domain> findByAccountId(String accountId);
    Domain findByIdAndAccountId(String domainId, String accountId);
}
