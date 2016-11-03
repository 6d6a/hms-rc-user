package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;

import ru.majordomo.hms.rc.user.common.PasswordManager;

public class FTPUser extends Resource implements Securable {
    private String passwordHash;
    private String homeDir;
    @Transient
    private UnixAccount unixAccount;

    public UnixAccount getUnixAccount() {
        return unixAccount;
    }

    public void setUnixAccount(UnixAccount unixAccount) {
        this.unixAccount = unixAccount;
    }

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

}
