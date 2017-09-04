package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.staff.resources.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.validation.UniqueNameResource;
import ru.majordomo.hms.rc.user.resources.validation.ValidRelativeFilePath;

@Document(collection = "ftpUsers")
@UniqueNameResource(FTPUser.class)
public class FTPUser extends Resource implements Securable {
    @NotBlank(message = "Пароль FTP пользователя не может быть пустым")
    private String passwordHash;

    @ValidRelativeFilePath
    private String homeDir = "";
    private List<Long> allowedIPAddresses;

    @Transient
    private UnixAccount unixAccount;

    @NotBlank(message = "Параметр unixAccount не может быть пустым")
    @ObjectId(value = UnixAccount.class, message = "Не найден UnixAccount с ID: ${validatedValue}")
    @Indexed
    private String unixAccountId;

    private Boolean allowWebFtp = true;

    public Boolean getAllowWebFtp() {
        return this.allowWebFtp;
    }

    public void setAllowWebFtp(Boolean enabled){
        this.allowWebFtp = enabled;
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
                Long ip = Network.ipAddressInStringToInteger(entry);
                if (!allowedIpsAsLong.contains(ip)) {
                    allowedIpsAsLong.add(ip);
                }
            }
            setAllowedIPAddresses(allowedIpsAsLong);
        }
    }

    @JsonIgnore
    public String getUnixAccountId() {
        return unixAccountId;
    }

    public void setUnixAccountId(String unixAccountId) {
        this.unixAccountId = unixAccountId;
    }

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

    @Override
    public String toString() {
        return "FTPUser{" +
                "passwordHash='" + passwordHash + '\'' +
                ", homeDir='" + homeDir + '\'' +
                ", allowedIPAddresses=" + allowedIPAddresses +
                ", unixAccount=" + unixAccount +
                ", unixAccountId='" + unixAccountId + '\'' +
                ", allowWebFtp='" + allowWebFtp + '\'' +
                "} " + super.toString();
    }
}
