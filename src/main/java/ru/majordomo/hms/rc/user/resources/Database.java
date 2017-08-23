package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.resources.validation.ObjectIdCollection;
import ru.majordomo.hms.rc.user.resources.validation.ServiceId;
import ru.majordomo.hms.rc.user.resources.validation.ValidDatabase;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseChecks;

@Document(collection = "databases")
@ValidDatabase(groups = DatabaseChecks.class)
public class Database extends Resource implements Serviceable, Quotable {
    @ServiceId(groups = DatabaseChecks.class)
    @NotNull(message = "serviceId не может быть пустым")
    @Indexed
    private String serviceId;

    @NotNull(message = "Тип базы не указан")
    private DBType type;

    private Long quota;
    private Long quotaUsed;
    private Boolean writable = true;

    @Transient
    private List<DatabaseUser> databaseUsers = new ArrayList<>();

    @ObjectIdCollection(value = DatabaseUser.class, groups = DatabaseChecks.class)
    private List<String> databaseUserIds = new ArrayList<>();

    public List<DatabaseUser> getDatabaseUsers() {
        return databaseUsers;
    }

    public void setDatabaseUsers(List<DatabaseUser> databaseUsers) {
        this.databaseUsers = databaseUsers;
        List<String> databaseUserIds = new ArrayList<>();
        for (DatabaseUser databaseUser: databaseUsers) {
            if (databaseUser.getId() != null) {
                databaseUserIds.add(databaseUser.getId());
            }
        }
        this.databaseUserIds = databaseUserIds;
    }

    public void addDatabaseUser(DatabaseUser databaseUser) {
        this.databaseUsers.add(databaseUser);
        if (!databaseUserIds.contains(databaseUser.getId())) {
            this.databaseUserIds.add(databaseUser.getId());
        }
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
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
}
