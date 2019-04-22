package ru.majordomo.hms.rc.user.resources.DTO;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;

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
}
