package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotBlank;
import org.jongo.marshall.jackson.oid.MongoId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import ru.majordomo.hms.rc.user.resources.validation.*;
import ru.majordomo.hms.rc.user.resources.validation.group.*;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = Database.class),
                @JsonSubTypes.Type(value = DatabaseUser.class),
                @JsonSubTypes.Type(value = DNSResourceRecord.class),
                @JsonSubTypes.Type(value = Domain.class),
                @JsonSubTypes.Type(value = FTPUser.class),
                @JsonSubTypes.Type(value = Mailbox.class),
                @JsonSubTypes.Type(value = Person.class),
                @JsonSubTypes.Type(value = Redirect.class),
                @JsonSubTypes.Type(value = ResourceArchive.class),
                @JsonSubTypes.Type(value = SSLCertificate.class),
                @JsonSubTypes.Type(value = UnixAccount.class),
                @JsonSubTypes.Type(value = WebSite.class)
        })
public abstract class Resource implements Hashable {
    @Id
    @MongoId
    private String id;

    @NotBlank(message = "Имя не может быть пустым",
              groups =
                      {
                              DatabaseChecks.class,
                              DatabaseImportChecks.class,
                              DatabaseUserChecks.class,
                              DatabaseUserImportChecks.class,
                              DnsRecordChecks.class,
                              DomainChecks.class,
                              FTPUserChecks.class,
                              MailboxChecks.class,
                              MailboxImportChecks.class,
//                              PersonChecks.class,
//                              PersonImportChecks.class,
                              SSLCertificateChecks.class,
                              SSLCertificateImportChecks.class,
                              UnixAccountChecks.class,
                              WebSiteChecks.class,
                              WebSiteImportChecks.class
                      })
    @Length(max = 16, message = "Имя не может быть длиннее 16 символов", groups = {DatabaseUserChecks.class, DatabaseUserImportChecks.class})
    @DomainName(groups = DomainChecks.class)
//    @ValidPersonName(groups = {PersonChecks.class})
    @ValidDatabaseName(groups = {DatabaseChecks.class})
    @ValidDatabaseUserName(groups = {DatabaseUserChecks.class})
    @ValidFTPUserName(groups = {FTPUserChecks.class})
    @ValidMailboxName(groups = {MailboxChecks.class, MailboxImportChecks.class})
    @ObjectId(value = Domain.class, fieldName = "name", groups = SSLCertificateChecks.class, message = "Домен с указанным именем не найден")
    @Indexed
    private String name;

    @NotBlank(message = "Аккаунт ID не может быть пустым",
              groups =
                      {
                              DatabaseChecks.class,
                              DatabaseImportChecks.class,
                              DatabaseUserChecks.class,
                              DatabaseUserImportChecks.class,
                              DomainChecks.class,
                              FTPUserChecks.class,
                              MailboxChecks.class,
                              MailboxImportChecks.class,
                              PersonChecks.class,
                              PersonImportChecks.class,
                              ResourceArchiveChecks.class,
                              SSLCertificateChecks.class,
                              SSLCertificateImportChecks.class,
                              UnixAccountChecks.class,
                              RedirectChecks.class,
                              WebSiteChecks.class,
                              WebSiteImportChecks.class
                      })
    @Indexed
    private String accountId;

    @Indexed
    Boolean switchedOn = true;

    @JsonIgnore
    private LocalDateTime lockedDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Indexed
    private LocalDateTime willBeDeletedAfter;

    public Boolean isWillBeDeleted() {
        return willBeDeletedAfter != null;
    }

    public Boolean isLocked() {
        return this.lockedDateTime != null;
    }

    public abstract void switchResource();

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

    public void setLocked(Boolean locked) {
        if (locked) {
            this.lockedDateTime = LocalDateTime.now();
        } else {
            this.lockedDateTime = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getWillBeDeletedAfter() {
        return willBeDeletedAfter;
    }

    public void setWillBeDeletedAfter(LocalDateTime willBeDeletedAfter) {
        this.willBeDeletedAfter = willBeDeletedAfter;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", switchedOn=" + switchedOn +
                ", lockedDateTime=" + lockedDateTime +
                '}';
    }
}
