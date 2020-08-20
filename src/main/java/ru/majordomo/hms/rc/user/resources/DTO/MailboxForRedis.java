package ru.majordomo.hms.rc.user.resources.DTO;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;

import javax.annotation.Nullable;

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

    @Nullable
    private String dkimSelector;
    /**
     * приватный ключ которым подписываются письма. Одна большая строка в формате:
     * -----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAnVjJ5X1M7WTNaAuLT294NPFu29msuJ3aj1xzsCbYyVfVxwSL\nnP ... lPquQ==\n-----END RSA PRIVATE KEY-----\n
     */
    @Nullable
    private String dkimKey;
}
