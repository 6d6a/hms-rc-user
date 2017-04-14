package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;

import ru.majordomo.hms.rc.user.validation.*;
import ru.majordomo.hms.rc.user.validation.group.DatabaseChecks;
import ru.majordomo.hms.rc.user.validation.group.DatabaseUserChecks;
import ru.majordomo.hms.rc.user.validation.group.DnsRecordChecks;
import ru.majordomo.hms.rc.user.validation.group.DomainChecks;
import ru.majordomo.hms.rc.user.validation.group.FTPUserChecks;
import ru.majordomo.hms.rc.user.validation.group.MailboxChecks;
import ru.majordomo.hms.rc.user.validation.group.PersonChecks;
import ru.majordomo.hms.rc.user.validation.group.ResourceArchiveChecks;
import ru.majordomo.hms.rc.user.validation.group.SSLCertificateChecks;
import ru.majordomo.hms.rc.user.validation.group.UnixAccountChecks;
import ru.majordomo.hms.rc.user.validation.group.WebSiteChecks;

import javax.validation.constraints.Pattern;

public abstract class Resource {
    @Id
    private String id;

    @NotBlank(message = "Имя не может быть пустым",
              groups =
                      {
                              DatabaseChecks.class,
                              DatabaseUserChecks.class,
                              DnsRecordChecks.class,
                              DomainChecks.class,
                              FTPUserChecks.class,
                              MailboxChecks.class,
                              PersonChecks.class,
                              SSLCertificateChecks.class,
                              UnixAccountChecks.class,
                              WebSiteChecks.class
                      })
    @Length(max = 16, message = "Имя не может быть длиннее 16 символов", groups = {DatabaseChecks.class, DatabaseUserChecks.class})
    @DomainName(groups = DomainChecks.class)
    @ValidPersonName(groups = {PersonChecks.class})
    @ValidDatabaseName(groups = {DatabaseChecks.class})
    @ValidDatabaseUserName(groups = {DatabaseUserChecks.class})
    @ValidFTPUser(groups = {FTPUserChecks.class})
    @ObjectId(value = Domain.class, fieldName = "name", groups = SSLCertificateChecks.class, message = "Домен с указанным именем не найден")
    private String name;

    @NotBlank(message = "Аккаунт ID не может быть пустым",
              groups =
                      {
                              DatabaseChecks.class,
                              DatabaseUserChecks.class,
                              DomainChecks.class,
                              FTPUserChecks.class,
                              MailboxChecks.class,
                              PersonChecks.class,
                              ResourceArchiveChecks.class,
                              SSLCertificateChecks.class,
                              UnixAccountChecks.class,
                              WebSiteChecks.class
                      })
    private String accountId;

    Boolean switchedOn = true;

    public abstract void switchResource();

    @JsonIgnore
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Boolean getSwitchedOn() {
        return switchedOn;
    }

    public void setSwitchedOn(Boolean switchedOn) {
        this.switchedOn = switchedOn;
    }

    public Boolean isSwitchedOn() {
        return switchedOn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", switchedOn=" + switchedOn +
                '}';
    }
}
