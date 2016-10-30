package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "unixAccounts")
public class UnixAccount extends Resource implements ServerStorable{
    @Indexed
    private Integer uid;
    private String homeDir;
    private String serverId;

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
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", uid=" + uid +
                ", homeDir='" + homeDir + '\'' +
                ", serverId=" + serverId +
                '}';
    }
}
