package ru.majordomo.hms.rc.user.repositoriesRedis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;

import javax.annotation.ParametersAreNonnullByDefault;

@Repository
@ParametersAreNonnullByDefault
public interface MailboxRedisRepository extends CrudRepository<MailboxForRedis, String> {
    default boolean isAggregator(String mailboxUserName, String domainUnicode) {
        String id = MailboxForRedis.getAggregatorRedisId(domainUnicode);
        String redirectAddresses = MailboxForRedis.getRedisId(mailboxUserName, domainUnicode);

        MailboxForRedis response = findById(id).orElse(null);
        return response != null && redirectAddresses.equals(response.getRedirectAddresses());
    }
}
