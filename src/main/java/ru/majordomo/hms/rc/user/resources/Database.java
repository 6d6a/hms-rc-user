package ru.majordomo.hms.rc.user.resources;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "databases")
public class Database extends Resource {
    private Long size;
    private ObjectId server;
    private DBType dbType;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public ObjectId getServer() {
        return server;
    }

    public void setServer(ObjectId server) {
        this.server = server;
    }

    public DBType getDbType() {
        return dbType;
    }

    public void setDbType(DBType dbType) {
        this.dbType = dbType;
    }
}
