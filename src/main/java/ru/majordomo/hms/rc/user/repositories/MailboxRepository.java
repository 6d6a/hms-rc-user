package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.Mailbox;

public interface MailboxRepository extends MongoRepository<Mailbox,String> {
}
