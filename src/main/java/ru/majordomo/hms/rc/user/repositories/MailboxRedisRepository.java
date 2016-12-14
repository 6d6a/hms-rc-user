package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;

@Repository
public interface MailboxRedisRepository extends CrudRepository<MailboxForRedis, String> {

}
