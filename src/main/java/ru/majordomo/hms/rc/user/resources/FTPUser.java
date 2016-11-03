package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;

import ru.majordomo.hms.rc.user.common.PasswordManager;

public class FTPUser extends Resource implements Securable, ServerStorable {
    private String passwordHash;
    private String homeDir;
    private String serverId;

    @Override
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        passwordHash = PasswordManager.forFtp(plainPassword);
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    @Override
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}
