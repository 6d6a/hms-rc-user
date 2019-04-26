package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
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
    private final static String CIDR_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/([1-9]|[1-2]\\d|3[0-2]))?$";

    @NotBlank(message = "Пароль FTP пользователя не может быть пустым")
    private String passwordHash;

    @ValidRelativeFilePath
    private String homeDir = "";
    private List<@Pattern(
            regexp = CIDR_PATTERN,
            message = "{ru.majordomo.hms.rc.user.resources.validation.ValidCidrOrIP.message}"
    ) String> allowedIPAddresses;

    @Transient
    private UnixAccount unixAccount;

    @NotBlank(message = "Параметр unixAccount не может быть пустым")
    @ObjectId(value = UnixAccount.class, message = "Не найден UnixAccount с ID: ${validatedValue}")
    @Indexed
    private String unixAccountId;

    private Boolean allowWebFtp = true;

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
}
