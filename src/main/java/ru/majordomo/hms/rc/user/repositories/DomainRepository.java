package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

public interface DomainRepository extends MongoRepository<Domain,String> {
    List<Domain> findByAccountId(String accountId);
    Domain findByIdAndAccountId(String domainId, String accountId);
    Domain findByNameAndAccountId(String name, String accountId);
    Domain findBySslCertificateId(String sslCertificateId);
    Domain findBySslCertificateIdAndAccountId(String sslCertificateId, String accountId);
    Domain findByName(String name);
    List<Domain> findByRegSpecPaidTillBetween(LocalDate start, LocalDate end);
    List<Domain> findByPersonId(String personId);
    List<Domain> findByPersonIdAndAccountId(String personId, String accountId);
    List<Domain> findByAccountIdAndRegSpecPaidTillBetween(String accountId, LocalDate start, LocalDate end);
    List<Domain> findByParentDomainId(String parentDomainId);
    List<Domain> findByParentDomainIdAndAccountId(String parentDomainId, String accountId);
    List<Domain> findByAccountIdAndParentDomainIdNotNull(String accountId);
    List<Domain> findByParentDomainIdNotNull();
    List<Domain> findByNameContains(String nameContains);

    @Query("{}")
    Stream<Domain> findAllStream();
}
