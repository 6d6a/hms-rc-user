package ru.majordomo.hms.rc.user.resources;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.resources.DTO.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ServiceId;
import ru.majordomo.hms.rc.user.resources.validation.ValidDatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserChecks;

@Document(collection = "databaseUsers")
@ValidDatabaseUser(groups = DatabaseUserChecks.class)
@Data
@EqualsAndHashCode(callSuper = true)
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

    @JsonIgnore
    private List<Long> allowedIPAddresses;

    private Map<String, Object> sessionVariables = new HashMap<>();

    @Transient
    @JsonIgnore
    private List<String> databaseIds;

    @DecimalMin(value = "0", message = "maxCpuTimePerSecond должно быть больше либо равно нулю или null")
    private BigDecimal maxCpuTimePerSecond;

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

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
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
}
