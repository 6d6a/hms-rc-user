package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.staff.resources.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Document(collection = "ftpUsers")
public class FTPUser extends Resource implements Securable {
    private String passwordHash;
    private String homeDir;
    private List<Long> allowedAddressList;
    @Transient
    private UnixAccount unixAccount;
    private String unixAccountId;

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
                    Long ip = Network.ipAddressInStringToInteger(entry);
                    if (!allowedIpsAsLong.contains(ip)) {
                        allowedIpsAsLong.add(ip);
                    }
                }
                setAllowedAddressList(allowedIpsAsLong);
            } catch (NumberFormatException e) {
                throw new ParameterValidateException("Неверный формат IP адреса");
            }
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

}
