package ru.majordomo.hms.rc.user.resources.DTO;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;

import javax.annotation.Nonnull;
import java.net.IDN;

/**
 * Объект для описания почтового ящика в redis, с которым работает exim. И настройки ящика для сбора почты.
 * Для хеша *@имя_домена устанавливающего ящик для сбора почты нужны только: redirectAddresses, writable, serverName
 */
@RedisHash("mailboxes")
@Data
public class MailboxForRedis {
    @Id
    private String id;
    private String name;
    private String passwordHash;
    private String blackList;
    private String whiteList;
    private String redirectAddresses;
    private Boolean mailFromAllowed;
    private Boolean antiSpamEnabled;
    private SpamFilterMood spamFilterMood;
    private SpamFilterAction spamFilterAction;
    private Boolean writable;
    private String serverName;
    private String storageData;
    private String allowedIps;

    static public String getRedisId(@Nonnull String mailboxUserName, @Nonnull String domainUnicode) {
        return mailboxUserName + "@" + IDN.toASCII(domainUnicode);
    }

    /** Возвращает id в redis для записи управляющей перенаправлением доменов с несуществующих ящиков */
    static public String getAggregatorRedisId(@Nonnull String domainUnicode) {
        return "*@" + IDN.toASCII(domainUnicode);
    }

    /**
     * Создать запись необходимую для работы ящика для сбора почты. Хэша в redis *@имя_домена.
     * @param mailboxUserName
     * @param domainNameUnicode
     * @param serverName
     * @param writable должен быть false если превышена квота, в остальных случаях true
     * @return
     */
    static public MailboxForRedis createAggregator(
            @Nonnull String mailboxUserName,
            @Nonnull String domainNameUnicode,
            @Nonnull String serverName,
            boolean writable
    ) {
        MailboxForRedis aggregatorRedis = new MailboxForRedis();
        aggregatorRedis.id = getAggregatorRedisId(domainNameUnicode);
        aggregatorRedis.redirectAddresses = getRedisId(mailboxUserName, domainNameUnicode);
        aggregatorRedis.writable = writable;
        aggregatorRedis.serverName = serverName;
        return aggregatorRedis;
    }
}
