package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.validation.ValidAbsoluteFilePath;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;
import ru.majordomo.hms.rc.user.resources.validation.ValidMailbox;
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxChecks;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;

@Document(collection = "mailboxes")
@ValidMailbox
public class Mailbox extends Resource implements ServerStorable, Quotable, Securable {
    @Transient
    @Valid
    @ConvertGroup(from = MailboxChecks.class, to = Default.class)
    @NotNull(message = "Для ящика должен быть указан домен")
    private Domain domain;

    @NotBlank(message = "Для ящика должен быть указан id домена")
    @ObjectId(Domain.class)
    private String domainId;

    @NotBlank(message = "Не указан пароль для почтового ящика")
    private String passwordHash;

    @Valid
    private List<@ValidEmail String> redirectAddresses = new ArrayList<>();

    @Valid
    private List<@ValidEmail String> blackList = new ArrayList<>();

    @Valid
    private List<@ValidEmail String> whiteList = new ArrayList<>();

    private Boolean mailFromAllowed = true;

    private Boolean antiSpamEnabled = false;

    private SpamFilterAction spamFilterAction = SpamFilterAction.MOVE_TO_SPAM_FOLDER;

    private SpamFilterMood spamFilterMood = SpamFilterMood.NEUTRAL;

    private String serverId;

    @Min(value = 0L, message = "Квота не может иметь отрицательное значение")
    @NotNull(message = "Квота не может быть равной null")
    private Long quota = 250000L;

    @Min(value = 0L, message = "Использованная квота не может иметь отрицательное значение")
    @NotNull(message = "Использованная квота не может быть равной null")
    private Long quotaUsed = 0L;

    private Boolean writable;

    private Boolean isAggregator;

    @ValidAbsoluteFilePath
    private String mailSpool;

    @NotNull(message = "Uid не может быть равным null")
    private Integer uid;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
        setDomainId(domain.getId());
    }

    @JsonIgnore
    public String getFullName() {
        if (domain != null) {
            return getName() + '@' + domain.getName();
        } else {
            return null;
        }
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        if (plainPassword != null && !plainPassword.equals("")) {
            passwordHash = PasswordManager.forMailStorage(plainPassword);
        }
    }

    public List<String> getRedirectAddresses() {
        return redirectAddresses;
    }

    public void setRedirectAddresses(List<String> redirectAddresses) {
        this.redirectAddresses = redirectAddresses;
    }

    public void addRedirectAddress(String emailAddress) {
        this.redirectAddresses.add(emailAddress);
    }

    public List<String> getBlackList() {
        return blackList;
    }

    public void setBlackList(List<String> blackList) {
        this.blackList = blackList;
    }

    public void addToBlackList(String emailAddress) {
        this.blackList.add(emailAddress);
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public void addToWhiteList(String emailAddress) {
        this.whiteList.add(emailAddress);
    }

    public Boolean getMailFromAllowed() { return mailFromAllowed; }

    public void setMailFromAllowed(Boolean mailFromAllowed) { this.mailFromAllowed = mailFromAllowed; }

    public Boolean getAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public void setAntiSpamEnabled(Boolean antiSpamEnabled) {
        this.antiSpamEnabled = antiSpamEnabled;
    }

    public SpamFilterAction getSpamFilterAction() {
        return spamFilterAction;
    }

    public void setSpamFilterAction(SpamFilterAction spamFilterAction) {
        this.spamFilterAction = spamFilterAction;
    }

    public SpamFilterMood getSpamFilterMood() {
        return spamFilterMood;
    }

    public void setSpamFilterMood(SpamFilterMood spamFilterMood) {
        this.spamFilterMood = spamFilterMood;
    }

    @JsonIgnore
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setQuota(Long quota) {
        this.quota = quota;
    }

    @Override
    public Long getQuota() {
        return quota;
    }

    @Override
    public void setQuotaUsed(Long quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    @Override
    public Long getQuotaUsed() {
        return quotaUsed == null ? 0L : quotaUsed;
    }

    @Override
    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    @Override
    public Boolean getWritable() {
        return writable;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    @Override
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Boolean getIsAggregator() {
        return isAggregator;
    }

    public void setIsAggregator(Boolean isAggregator) {
        this.isAggregator = isAggregator;
    }

    public String getMailSpool() {
        return mailSpool;
    }

    public void setMailSpool(String mailSpool) {
        this.mailSpool = mailSpool;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }
}
