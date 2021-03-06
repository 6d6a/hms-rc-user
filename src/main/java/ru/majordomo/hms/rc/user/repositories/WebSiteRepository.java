package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import java.util.List;

public interface WebSiteRepository extends MongoRepository<WebSite,String> {
    List<WebSite> findByAccountId(String accountId);
    List<WebSite> findByUnixAccountId(String unixAccountId);
    WebSite findByIdAndAccountId(String websiteId, String accountId);
    Long countByAccountId(String accountId);
    WebSite findByDomainIdsContains(String domainId);
    WebSite findByDomainIdsContainsAndAccountId(String domainId, String accountId);
    List<WebSite> findByServiceId(String serviceId);
    List<WebSite> findByServiceIdAndAccountId(String serviceId, String accountId);
}
