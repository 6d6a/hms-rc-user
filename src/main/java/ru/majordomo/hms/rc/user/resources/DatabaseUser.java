package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import ru.majordomo.hms.rc.staff.resources.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public class DatabaseUser extends Resource implements Serviceable, Securable {
    private String passwordHash;
    private DBType type;
    private String serviceId;
    private List<Long> allowedAddressList;

    @JsonIgnore
    public List<Long> getAllowedAddressList() {
        return allowedAddressList;
    }

    @JsonGetter(value = "allowedAddressList")
    public List<String> getAllowedIpsAsString() {
        List<String> allowedIpsAsString = new ArrayList<>();
        if (allowedAddressList != null) {
            for (Long entry : allowedAddressList) {
                allowedIpsAsString.add(Network.ipAddressInIntegerToString(entry));
            }
        }
        return allowedIpsAsString;
    }

    @JsonIgnore
    public void setAllowedAddressList(List<Long> allowedAddressList) {
        this.allowedAddressList = allowedAddressList;
    }

    @JsonSetter(value = "allowedAddressList")
    public void setAllowedIpsAsString(List<String> allowedIpsAsString) {
        List<Long> allowedIpsAsLong = new ArrayList<>();
        if (allowedIpsAsString != null) {
            try {
                for (String entry : allowedIpsAsString) {
                    allowedIpsAsLong.add(Network.ipAddressInStringToInteger(entry));
                }
                setAllowedAddressList(allowedIpsAsLong);
            } catch (NumberFormatException e) {
                throw new ParameterValidateException("Неверный формат IP адреса");
            }
        }
    }

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
