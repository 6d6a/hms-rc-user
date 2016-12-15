package ru.majordomo.hms.rc.user.resources.DTO;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;

@RedisHash("mailboxes")
public class MailboxForRedis {
    @Id
    private String name;
    private String passwordHash;
    private String blackList;
    private String whiteList;
    private String redirectAddresses;
    private Boolean antiSpamEnabled;
    private SpamFilterMood spamFilterMood;
    private SpamFilterAction spamFilterAction;
    private Boolean writable;
    private String serverName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getBlackList() {
        return blackList;
    }

    public void setBlackList(String blackList) {
        this.blackList = blackList;
    }

    public String getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(String whiteList) {
        this.whiteList = whiteList;
    }

    public String getRedirectAddresses() {
        return redirectAddresses;
    }

    public void setRedirectAddresses(String redirectAddresses) {
        this.redirectAddresses = redirectAddresses;
    }

    public Boolean getAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public void setAntiSpamEnabled(Boolean antiSpamEnabled) {
        this.antiSpamEnabled = antiSpamEnabled;
    }

    public SpamFilterMood getSpamFilterMood() {
        return spamFilterMood;
    }

    public void setSpamFilterMood(SpamFilterMood spamFilterMood) {
        this.spamFilterMood = spamFilterMood;
    }

    public SpamFilterAction getSpamFilterAction() {
        return spamFilterAction;
    }

    public void setSpamFilterAction(SpamFilterAction spamFilterAction) {
        this.spamFilterAction = spamFilterAction;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
