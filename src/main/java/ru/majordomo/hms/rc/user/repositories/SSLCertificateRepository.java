package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificateState;

import java.time.LocalDateTime;
import java.util.List;

public interface SSLCertificateRepository extends MongoRepository<SSLCertificate,String> {
    List<SSLCertificate> findByState(@Param("state") SSLCertificateState state);
    List<SSLCertificate> findByStateIn(@Param("states") List<SSLCertificateState> states);
    List<SSLCertificate> findByNotAfterLessThan(@Param("dateTime") LocalDateTime dateTime);
    List<SSLCertificate> findByAccountId(String accountId);
    SSLCertificate findByIdAndAccountId(String resourceId, String accountId);
    SSLCertificate findByName(String name);
}