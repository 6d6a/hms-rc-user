package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.rc.user.resources.Redirect;

import java.util.List;

public interface RedirectRepository extends MongoRepository<Redirect,String> {
    List<Redirect> findByAccountId(String accountId);
    Redirect findByIdAndAccountId(String websiteId, String accountId);
    Redirect findByDomainId(String domainId);
    Redirect findByDomainIdAndAccountId(String domainId, String accountId);
    List<Redirect> findByServiceId(String serviceId);
    List<Redirect> findByServiceIdAndAccountId(String serviceId, String accountId);
}
