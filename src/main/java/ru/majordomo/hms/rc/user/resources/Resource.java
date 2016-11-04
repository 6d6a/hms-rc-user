package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Id;

public abstract class Resource {
    @Id
    private String id;
    private String name;
    private String accountId;
    Boolean switchedOn = true;

    public abstract void switchResource();

    @JsonIgnore
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Boolean getSwitchedOn() {
        return switchedOn;
    }

    public void setSwitchedOn(Boolean switchedOn) {
        this.switchedOn = switchedOn;
    }

    public Boolean isSwitchedOn() {
        return switchedOn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return  "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", switchedOn=" + switchedOn;
    }
}
