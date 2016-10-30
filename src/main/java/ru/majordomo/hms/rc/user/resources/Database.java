package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "databases")
public class Database extends Resource implements ServerStorable {
    private Double size;
    private String serverId;
    private DBType type;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
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
}
