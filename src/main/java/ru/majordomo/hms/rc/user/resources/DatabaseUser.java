package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public class DatabaseUser extends Resource implements Securable {
    private String passwordHash;
    private DBType type;

    public DBType getType() {
        return type;
    }

    public void setType(DBType type) {
        this.type = type;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        if (type == null) {
            throw new ParameterValidateException("Не могу определить тип хеширования, т.к. тип не установлен");
        }
        switch (type) {
            case MYSQL:
                passwordHash = PasswordManager.forMySQL5(plainPassword);
                break;
            case POSTGRES:
                passwordHash = PasswordManager.forPostgres(plainPassword);
                break;
            default:
                throw new ParameterValidateException("Неизвестный тип базы");
        }
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }
}
