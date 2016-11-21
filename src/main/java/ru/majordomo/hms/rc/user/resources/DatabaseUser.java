package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;

import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public class DatabaseUser extends Resource implements Serviceable, Securable {
    private String passwordHash;
    private DBType type;
    private String serviceId;

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
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
