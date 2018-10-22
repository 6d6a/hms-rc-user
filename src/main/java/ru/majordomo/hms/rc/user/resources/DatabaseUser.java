package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.validation.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.staff.resources.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ServiceId;
import ru.majordomo.hms.rc.user.resources.validation.ValidDatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserChecks;

@Document(collection = "databaseUsers")
@ValidDatabaseUser(groups = DatabaseUserChecks.class)
public class DatabaseUser extends Resource implements Serviceable, Securable {
    @NotBlank(message = "Пароль не может быть пустым")
    private String passwordHash;

    @NotNull(message = "Тип не может быть пустым")
    @Indexed
    private DBType type;

    @ServiceId(groups = DatabaseUserChecks.class)
    @NotNull(message = "serviceId не может быть пустым")
    @Indexed
    private String serviceId;

    private List<Long> allowedIPAddresses;

    @Transient
    private List<String> databaseIds;

    @DecimalMin(value = "0", message = "maxCpuTimePerSecond должно быть больше либо равно нулю или null")
    private BigDecimal maxCpuTimePerSecond;

    @JsonIgnore
    public List<String> getDatabaseIds() {
        return databaseIds;
    }
    @JsonIgnore
    public void setDatabaseIds(List<String> databaseIds) {
        this.databaseIds = databaseIds;
    }

    @JsonIgnore
    public List<Long> getAllowedIPAddresses() {
        return allowedIPAddresses;
    }

    @JsonGetter(value = "allowedIPAddresses")
    public List<String> getAllowedIpsAsCollectionOfString() {
        List<String> allowedIpsAsString = new ArrayList<>();
        if (allowedIPAddresses != null) {
            for (Long entry : allowedIPAddresses) {
                allowedIpsAsString.add(Network.ipAddressInIntegerToString(entry));
            }
        }
        return allowedIpsAsString;
    }

    @JsonIgnore
    public void setAllowedIPAddresses(List<Long> allowedIPAddresses) {
        this.allowedIPAddresses = allowedIPAddresses;
    }

    @JsonSetter(value = "allowedIPAddresses")
    public void setAllowedIpsAsCollectionOfString(List<String> allowedIpsAsString) throws NumberFormatException {
        List<Long> allowedIpsAsLong = new ArrayList<>();
        if (allowedIpsAsString != null) {
            for (String entry : allowedIpsAsString) {
                allowedIpsAsLong.add(Network.ipAddressInStringToInteger(entry));
            }
            setAllowedIPAddresses(allowedIpsAsLong);
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
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException, IllegalArgumentException {
        if (type == null) {
            throw new IllegalArgumentException("Не могу определить тип хеширования, т.к. тип не установлен");
        }
        switch (type) {
            case MYSQL:
                passwordHash = PasswordManager.forMySQL5(plainPassword);
                break;
            case POSTGRES:
                passwordHash = PasswordManager.forPostgres(plainPassword);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип базы");
        }
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return "DatabaseUser{" +
                "passwordHash='" + passwordHash + '\'' +
                ", type=" + type +
                ", serviceId='" + serviceId + '\'' +
                ", allowedIPAddresses=" + allowedIPAddresses +
                ", databaseIds=" + databaseIds +
                "} " + super.toString();
    }

    public BigDecimal getMaxCpuTimePerSecond() {
        return maxCpuTimePerSecond;
    }

    public void setMaxCpuTimePerSecond(BigDecimal maxCpuTimePerSecond) {
        this.maxCpuTimePerSecond = maxCpuTimePerSecond;
    }
}
