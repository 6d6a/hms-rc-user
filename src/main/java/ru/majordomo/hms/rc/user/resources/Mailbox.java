package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.*;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Document(collection = "mailboxes")
@ValidMailbox
public class Mailbox extends Resource implements ServerStorable, Quotable, Securable {
    @Transient
    private Domain domain;

    @NotBlank(message = "Для ящика должен быть указан id домена")
    @ObjectId(Domain.class)
    @Indexed
    private String domainId;

    @NotBlank(message = "Не указан пароль для почтового ящика")
    private String passwordHash;

    @Length(
            max = 128,
            message = "Поле 'Комментарий' не может быть длиннее {max} символов")
    private String comment;

    @Valid
    private List<@ValidEmail String> redirectAddresses = new ArrayList<>();

    @Valid
    private List<@ValidEmailOrDomainName String> blackList = new ArrayList<>();

    @Valid
    private List<@ValidEmailOrDomainName String> whiteList = new ArrayList<>();

    private Boolean mailFromAllowed = true;

    private Boolean antiSpamEnabled = false;

    private SpamFilterAction spamFilterAction = SpamFilterAction.MOVE_TO_SPAM_FOLDER;

    private SpamFilterMood spamFilterMood = SpamFilterMood.NEUTRAL;

    @Indexed
    private String serverId;

    @Min(value = 0L, message = "Квота не может иметь отрицательное значение")
    @NotNull(message = "Квота не может быть равной null")
    private Long quota = 250000L;

    @Min(value = 0L, message = "Использованная квота не может иметь отрицательное значение")
    @NotNull(message = "Использованная квота не может быть равной null")
    private Long quotaUsed = 0L;

    private Boolean writable;

    @Indexed
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

    @JsonIgnore
    public String getFullNameInPunycode() {
        if (domain != null) {
            return getName() + '@' + java.net.IDN.toASCII(domain.getName());
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getRedirectAddresses() {
        return redirectAddresses;
    }

    @JsonIgnore
    public List<String> getRedirectAddressesInPunycode() {
        return getPunycodedList(redirectAddresses);
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

    @JsonIgnore
    public List<String> getBlackListInPunycode() {
        return this.getPunycodedList(blackList);
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

    @JsonIgnore
    public List<String> getWhiteListInPunycode() {
        return this.getPunycodedList(whiteList);
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public void addToWhiteList(String emailAddress) {
        this.whiteList.add(emailAddress);
    }

    private List<String> getPunycodedList(List<String> notPunycodedList) {
        List<String> punycidedList= new ArrayList<>();
        for (String elem : notPunycodedList) {
            String[] afterSplit = elem.split("@");
            if (afterSplit.length == 2) {
                try {
                    punycidedList.add(IDN.toASCII(afterSplit[0]) + "@" + IDN.toASCII(afterSplit[1]));
                } catch (Exception ignored) {}
            } else {
                try {
                    punycidedList.add(IDN.toASCII(afterSplit[0]));
                } catch (Exception ignored) {}
            }
        }
        return punycidedList;
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
    public Long getQuota() { return quota; }

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

    @JsonIgnore
    public String getMailSpoolInPunycode() {

        String[] labels = mailSpool.split("/");
        for (int i = 0; i < labels.length; i++) {
            labels[i] = java.net.IDN.toASCII(labels[i]);
        }
        String mailSpoolinPunycode = String.join("/", labels);
        return String.join("/", mailSpoolinPunycode);
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

    @Override
    public String toString() {
        return "Mailbox{" +
                "domain=" + domain +
                ", domainId='" + domainId + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", comment='" + comment + '\'' +
                ", redirectAddresses=" + redirectAddresses +
                ", blackList=" + blackList +
                ", whiteList=" + whiteList +
                ", mailFromAllowed=" + mailFromAllowed +
                ", antiSpamEnabled=" + antiSpamEnabled +
                ", spamFilterAction=" + spamFilterAction +
                ", spamFilterMood=" + spamFilterMood +
                ", serverId='" + serverId + '\'' +
                ", quota=" + quota +
                ", quotaUsed=" + quotaUsed +
                ", writable=" + writable +
                ", isAggregator=" + isAggregator +
                ", mailSpool='" + mailSpool + '\'' +
                ", uid=" + uid +
                "} " + super.toString();
    }
}
