package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "unixAccounts")
public class UnixAccount extends Resource implements ServerStorable, Quotable{
    @Indexed
    private Integer uid;
    private String homeDir;
    private String serverId;
    private Long quota;
    private Long quotaUsed;
    private Boolean writable;

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

    @Override
    public String toString() {
        return "UnixAccount{" +
                "id=" + getId() +
                ", name=" + getName() +
                ", switchedOn=" + getSwitchedOn() +
                ", uid=" + uid +
                ", homeDir='" + homeDir + '\'' +
                ", serverId='" + serverId + '\'' +
                ", quota=" + quota +
                ", quotaUsed=" + quotaUsed +
                ", writable=" + writable +
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
        return quotaUsed;
    }

    @Override
    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    @Override
    public Boolean getWritable() {
        return writable;
    }
}
