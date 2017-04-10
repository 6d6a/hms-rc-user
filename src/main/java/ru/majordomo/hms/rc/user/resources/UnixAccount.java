package ru.majordomo.hms.rc.user.resources;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.validation.ValidHomeDir;

@Document(collection = "unixAccounts")
public class UnixAccount extends Resource implements ServerStorable, Quotable, Securable {
    @Transient
    public static final int MIN_UID = 2000;
    @Transient
    public static final int MAX_UID = 65535;

    @Indexed
    @NotNull(message = "Uid не может быть равным null")
    @Range(min = MIN_UID, max = MAX_UID)
    private Integer uid;

    @NotBlank(message = "homedir не может быть пустым")
    @ValidHomeDir
    private String homeDir;

    private String serverId;

    @Min(value = 0L, message = "Квота не может иметь отрицательное значение")
    @NotNull(message = "Квота не может быть равной null")
    private Long quota;

    @Min(value = 0L, message = "Использованная квота не может иметь отрицательное значение")
    @NotNull(message = "Использованная квота не может быть равной null")
    private Long quotaUsed;

    private Boolean writable;
    private Boolean sendmailAllowed;
    private String passwordHash;
    private SSHKeyPair keyPair;
    private List<CronTask> crontab = new ArrayList<>();

    public List<CronTask> getCrontab() {
        return crontab;
    }

    public void setCrontab(List<CronTask> crontab) {
        this.crontab = crontab;
    }

    public void addCronTask(CronTask task) {
        this.crontab.add(task);
    }

    public void delCronTask(CronTask task) {
        this.crontab.remove(task);
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public SSHKeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(SSHKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Override
    public String toString() {
        return "UnixAccount{" +
                super.toString() +
                ", uid=" + uid +
                ", homeDir='" + homeDir + '\'' +
                ", serverId='" + serverId + '\'' +
                ", quota=" + quota +
                ", quotaUsed=" + quotaUsed +
                ", writable=" + writable +
                ", passwordHash='" + passwordHash + '\'' +
                ", keyPair=" + keyPair +
                ", crontab=" + crontab +
                '}';
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

    public Boolean getSendmailAllowed() {
        return sendmailAllowed;
    }

    public void setSendmailAllowed(Boolean sendmailAllowed) {
        this.sendmailAllowed = sendmailAllowed;
    }

    @Override
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        passwordHash = PasswordManager.forUbuntu(plainPassword);
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }

}
