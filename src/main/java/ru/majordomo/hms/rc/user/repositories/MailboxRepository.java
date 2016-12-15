package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.Mailbox;

import java.util.List;

public interface MailboxRepository extends MongoRepository<Mailbox,String> {
    List<Mailbox> findByAccountId(String accountId);
    Mailbox findByIdAndAccountId(String mailboxId, String accountId);
    List<Mailbox> findByDomainId(String domainId);
    Mailbox findByDomainIdAndIsAggregator(String domainId, Boolean isAggregator);
    Mailbox findByNameAndDomainId(String name, String domainId);
}
