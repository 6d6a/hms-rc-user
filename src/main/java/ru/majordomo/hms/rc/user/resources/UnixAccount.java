package ru.majordomo.hms.rc.user.resources;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "unixAccounts")
public class UnixAccount extends Resource {
    @Indexed
    private Integer uid;
    private String homeDir;
    private ObjectId hostingServer;

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

    public ObjectId getHostingServer() {
        return hostingServer;
    }

    public void setHostingServer(ObjectId hostingServer) {
        this.hostingServer = hostingServer;
    }

    @Override
    public String toString() {
        return "UnixAccount{" +
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", uid=" + uid +
                ", homeDir='" + homeDir + '\'' +
                ", hostingServer=" + hostingServer +
                '}';
    }
}
