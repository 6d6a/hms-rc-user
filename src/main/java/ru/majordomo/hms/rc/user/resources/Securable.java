package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;

public interface Securable {
    void setPasswordHash(String passwordHash);
    void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException;
    String getPasswordHash();
}
