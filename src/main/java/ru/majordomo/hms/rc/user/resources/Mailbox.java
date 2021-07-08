package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.resources.validation.*;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "mailboxes")
@ValidMailbox
public class Mailbox extends Resource implements ServerStorable, Quotable, Securable {
    @JsonIgnore
    private final static String CIDR_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])/([1-9]|[1-2]\\d|3[0-2])$";

    @Transient
    private Domain domain;

    @NotBlank(message = "Для ящика должен быть указан id домена")
    @ObjectId(Domain.class)
    @Indexed
    private String domainId;

    @NotBlank(message = "Не указан пароль для почтового ящика")
    private String passwordHash;

    @Length(
            max = 128,
            message = "Поле 'Комментарий' не может быть длиннее {max} символов")
    private String comment;

    @Valid
    private List<@ValidEmail String> redirectAddresses = new ArrayList<>();

    @Valid
    private List<@ValidEmailOrDomainName String> blackList = new ArrayList<>();

    @Valid
    private List<@ValidEmailOrDomainName String> whiteList = new ArrayList<>();

    private Boolean mailFromAllowed = true;

    private Boolean antiSpamEnabled = false;

    private SpamFilterAction spamFilterAction = SpamFilterAction.MOVE_TO_SPAM_FOLDER;

    private SpamFilterMood spamFilterMood = SpamFilterMood.NEUTRAL;

    @Indexed
    private String serverId;

    @Min(value = 0L, message = "Квота не может иметь отрицательное значение")
    @NotNull(message = "Квота не может быть равной null")
    private Long quota = 250000L;

    @Min(value = 0L, message = "Использованная квота не может иметь отрицательное значение")
    @NotNull(message = "Использованная квота не может быть равной null")
    private Long quotaUsed = 0L;

    private Boolean writable;

    /** null - не менять агрегатор */
    @Indexed
    @Nullable
    private Boolean isAggregator;

    @ValidAbsoluteFilePath
    private String mailSpool;

    @NotNull(message = "Uid не может быть равным null")
    private Integer uid;

    private Set<@Pattern(regexp = CIDR_PATTERN, message = "{ru.majordomo.hms.rc.user.resources.validation.ValidCidr.message}") String> allowedIps = new HashSet<>();

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
        setDomainId(domain.getId());
    }

    @JsonIgnore
    public String getFullName() {
        if (domain != null) {
            return getName() + '@' + domain.getName();
        } else {
            return null;
        }
    }

    @Nullable
    @JsonIgnore
    public String getFullNameInPunycode() {
        if (domain != null) {
            return getName() + '@' + java.net.IDN.toASCII(domain.getName());
        } else {
            return null;
        }
    }

    public void setPasswordHashByPlainPassword(String plainPassword) throws UnsupportedEncodingException {
        if (plainPassword != null && !plainPassword.equals("")) {
            passwordHash = PasswordManager.forMailStorage(plainPassword);
        }
    }

    @JsonIgnore
    public List<String> getRedirectAddressesInPunycode() {
        return getPunycodedList(redirectAddresses);
    }

    public void addRedirectAddress(String emailAddress) {
        this.redirectAddresses.add(emailAddress);
    }

    @JsonIgnore
    public List<String> getBlackListInPunycode() {
        return this.getPunycodedList(blackList);
    }

    public void addToBlackList(String emailAddress) {
        this.blackList.add(emailAddress);
    }

    @JsonIgnore
    public List<String> getWhiteListInPunycode() {
        return this.getPunycodedList(whiteList);
    }

    public void addToWhiteList(String emailAddress) {
        this.whiteList.add(emailAddress);
    }

    private List<String> getPunycodedList(List<String> notPunycodedList) {
        List<String> punycidedList= new ArrayList<>();
        for (String elem : notPunycodedList) {
            String[] afterSplit = elem.split("@");
            if (afterSplit.length == 2) {
                try {
                    punycidedList.add(IDN.toASCII(afterSplit[0]) + "@" + IDN.toASCII(afterSplit[1]));
                } catch (Exception ignored) {}
            } else {
                try {
                    punycidedList.add(IDN.toASCII(afterSplit[0]));
                } catch (Exception ignored) {}
            }
        }
        return punycidedList;
    }

    @Override
    public Long getQuotaUsed() {
        return quotaUsed == null ? 0L : quotaUsed;
    }

    @JsonIgnore
    public String getMailSpoolInPunycode() {

        String[] labels = mailSpool.split("/");
        for (int i = 0; i < labels.length; i++) {
            labels[i] = java.net.IDN.toASCII(labels[i]);
        }
        String mailSpoolinPunycode = String.join("/", labels);
        return String.join("/", mailSpoolinPunycode);
    }

    @Override
    public List<Integer> hashes() {
        return Collections.singletonList(Objects.hash(
                this.getName(),
                this.getClass().getName(),
                this.getDomainId()
        ));
    }
}
