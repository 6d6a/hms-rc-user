package ru.majordomo.hms.rc.user.resources;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.ValidAbsoluteFilePath;
import ru.majordomo.hms.rc.user.resources.validation.ValidHomeDir;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "unixAccounts")
public class UnixAccount extends Resource implements ServerStorable, Quotable, Securable {
    @Transient
    public static final int MIN_UID = 1000;
    @Transient
    public static final int MAX_UID = 700000;

    @Indexed
    @NotNull(message = "Uid не может быть равным null")
    @Range(min = MIN_UID, max = MAX_UID)
    private Integer uid;

    @NotBlank(message = "homedir не может быть пустым")
    @ValidHomeDir
    @ValidAbsoluteFilePath
    private String homeDir;

    @Indexed
    private String serverId;

    /**
     * Дисковая квота в байтах
     */
    @Min(value = 0L, message = "Квота не может иметь отрицательное значение")
    @NotNull(message = "Квота не может быть равной null")
    private Long quota;

    /**
     * Дисковая квота в байтах
     */
    @Min(value = 0L, message = "Использованная квота не может иметь отрицательное значение")
    @NotNull(message = "Использованная квота не может быть равной null")
    private Long quotaUsed;

    private Boolean writable;
    private Boolean sendmailAllowed;
    private String passwordHash;
    private SSHKeyPair keyPair;
    private List<CronTask> crontab = new ArrayList<>();

    @Transient
    private Boolean infected;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public Long getQuotaUsed() {
        return quotaUsed == null ? 0L : quotaUsed;
    }

    @Override
    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        passwordHash = PasswordManager.forUbuntu(plainPassword);
    }
}
