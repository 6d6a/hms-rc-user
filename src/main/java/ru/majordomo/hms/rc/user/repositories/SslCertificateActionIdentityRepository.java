package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.rc.user.resources.DTO.SslCertificateActionIdentity;

public interface SslCertificateActionIdentityRepository extends MongoRepository<SslCertificateActionIdentity, String> {
    SslCertificateActionIdentity findBySslCertificateId(String sslCertificateId);
}
