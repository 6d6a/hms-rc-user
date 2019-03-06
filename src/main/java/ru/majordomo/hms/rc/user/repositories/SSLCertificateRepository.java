package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import java.util.List;

public interface SSLCertificateRepository extends MongoRepository<SSLCertificate,String> {
    List<SSLCertificate> findByAccountId(String accountId);
    SSLCertificate findByIdAndAccountId(String resourceId, String accountId);
    SSLCertificate findByNameAndAccountId(String name, String accountId);
    boolean existsByIdAndAccountId(String resourceId, String accountId);
    boolean existsByName(String name);
    boolean existsByNameAndAccountId(String name, String accountId);
}