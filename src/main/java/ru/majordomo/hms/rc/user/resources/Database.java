package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "databases")
public class Database extends Resource implements ServerStorable, Quotable{
    private String serverId;
    private DBType type;
    private Long quota;
    private Long quotaUsed;
    private Boolean writable;
    @Transient
    private List<DatabaseUser> databaseUsers = new ArrayList<>();
    private List<String> databaseUserIds = new ArrayList<>();

    public List<DatabaseUser> getDatabaseUsers() {
        return databaseUsers;
    }

    public void setDatabaseUsers(List<DatabaseUser> databaseUsers) {
        this.databaseUsers = databaseUsers;
        for (DatabaseUser databaseUser: databaseUsers) {
            if (databaseUser.getId() != null) {
                databaseUserIds.add(databaseUser.getId());
            }
        }
    }

    public void addDatabaseUser(DatabaseUser databaseUser) {
        this.databaseUserIds.add(databaseUser.getId());
        this.databaseUsers.add(databaseUser);
    }

    @JsonIgnore
    public List<String> getDatabaseUserIds() {
        return databaseUserIds;
    }

    public void setDatabaseUserIds(List<String> databaseUserIds) {
        this.databaseUserIds = databaseUserIds;
    }

    public void addDatabaseUserId(String databaseUserId) {
        if (!databaseUserIds.contains(databaseUserId)) {
            databaseUserIds.add(databaseUserId);
        }
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    @Override
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public DBType getType() {
        return type;
    }

    public void setType(DBType type) {
        this.type = type;
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
