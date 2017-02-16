package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.common.PasswordManager;

@Document(collection = "unixAccounts")
public class UnixAccount extends Resource implements ServerStorable, Quotable, Securable {
    @Indexed
    private Integer uid;
    private String homeDir;
    private String serverId;
    private Long quota;
    private Long quotaUsed;
    private Boolean writable;
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
