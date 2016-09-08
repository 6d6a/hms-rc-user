package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.LegalEntity;

public interface LegalEntityRepository extends MongoRepository<LegalEntity,String> {
}
