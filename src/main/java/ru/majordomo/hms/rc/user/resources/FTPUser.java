package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.resources.DTO.Network;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.validation.UniqueNameResource;
import ru.majordomo.hms.rc.user.resources.validation.ValidFTPUser;
import ru.majordomo.hms.rc.user.resources.validation.ValidRelativeFilePath;

@Document(collection = "ftpUsers")
@UniqueNameResource(FTPUser.class)
@Data
@EqualsAndHashCode(callSuper = true)
@ValidFTPUser
public class FTPUser extends Resource implements Securable {
    @JsonIgnore
//    private final static String CIDR_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/([1-9]|[1-2]\\d|3[0-2]))?$";
    private final static String CIDR_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    @NotBlank(message = "Пароль FTP пользователя не может быть пустым")
    private String passwordHash;

    @ValidRelativeFilePath
    private String homeDir = "";
    private List<Object> allowedIPAddresses;

    @Transient
    private UnixAccount unixAccount;

    @NotBlank(message = "Параметр unixAccount не может быть пустым")
    @ObjectId(value = UnixAccount.class, message = "Не найден UnixAccount с ID: ${validatedValue}")
    @Indexed
    private String unixAccountId;

    private Boolean allowWebFtp = true;

    public List<String> getAllowedIPAddresses() {
        return listToListIpsInString(allowedIPAddresses);
    }

    public void setAllowedIPAddresses(List<Object> allowedIPAddresses) {
        this.allowedIPAddresses = new ArrayList<>(listToListIpsInString(allowedIPAddresses));
    }

    @JsonIgnore
    public String getUnixAccountId() {
        return unixAccountId;
    }

    @Override
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        passwordHash = PasswordManager.forFtp(plainPassword);
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    private List<String> listToListIpsInString(List<Object> list) {
        Pattern pattern = Pattern.compile(CIDR_PATTERN);

        if (list == null) return new ArrayList<>();
        return list.stream().map(o -> {
            if (o instanceof Long) {
                return Network.ipAddressInIntegerToString((Long) o);
            } else if (o instanceof Integer) {
                return Network.ipAddressInIntegerToString(((Integer) o).longValue());
            } else if (o instanceof String && pattern.matcher((String) o).find()) {
                return (String) o;
            } else {
                throw new NumberFormatException("Некорректное значение IP-адреса: " + o);
            }
        }).collect(Collectors.toList());
    }
}
