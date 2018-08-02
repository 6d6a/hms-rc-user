package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;

import ru.majordomo.hms.rc.user.resources.validation.UniqueNameResource;
import ru.majordomo.hms.rc.user.resources.validation.group.DomainChecks;

@Document(collection = "domains")
@UniqueNameResource(Domain.class)
public class Domain extends Resource {

    @Transient
    @Valid
    @ConvertGroup(from = DomainChecks.class, to = Default.class)
    private Person person;

    @Transient
    @Valid
    @ConvertGroup(from = DomainChecks.class, to = Default.class)
    private SSLCertificate sslCertificate;

    @Indexed
    private String personId;

    private RegSpec regSpec;
    private List<DNSResourceRecord> dnsResourceRecords = new ArrayList<>();

    @Indexed
    private String sslCertificateId;

    private Boolean autoRenew = false;

    @Indexed
    private String parentDomainId;

    private LocalDateTime synced;

    @JsonIgnore
    private LocalDateTime needSync;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public RegSpec getRegSpec() {
        return regSpec;
    }

    public void setRegSpec(RegSpec regSpec) {
        this.regSpec = regSpec;
    }

    public List<DNSResourceRecord> getDnsResourceRecords() {
        return dnsResourceRecords;
    }

    public void setDnsResourceRecords(List<DNSResourceRecord> dnsResourceRecords) {
        this.dnsResourceRecords = dnsResourceRecords;
    }

    public void addDnsResourceRecord(DNSResourceRecord resourceRecord) {
        dnsResourceRecords.add(resourceRecord);
    }

    public void delDnsResourceRecord(DNSResourceRecord resourceRecord) {
        dnsResourceRecords.remove(resourceRecord);
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        if (person != null) {
            this.person = person;
            this.personId = person.getId();
        }
    }

    public SSLCertificate getSslCertificate() {
        return sslCertificate;
    }

    public void setSslCertificate(SSLCertificate sslCertificate) {
        this.sslCertificate = sslCertificate;
        if (this.sslCertificate != null) {
            this.sslCertificateId = sslCertificate.getId();
        }
    }

    @JsonIgnore
    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    @JsonIgnore
    public String getSslCertificateId() {
        return sslCertificateId;
    }

    public void setSslCertificateId(String sslCertificateId) {
        this.sslCertificateId = sslCertificateId;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public String getParentDomainId() {
        return parentDomainId;
    }

    public void setParentDomainId(String parentDomainId) {
        this.parentDomainId = parentDomainId;
    }

    public LocalDateTime getSynced() {
        return synced;
    }

    public void setSynced(LocalDateTime synced) {
        this.synced = synced;
    }

    public LocalDateTime getNeedSync() {
        return needSync;
    }

    public void setNeedSync(LocalDateTime needSync) {
        this.needSync = needSync;
    }

    @Override
    public String toString() {
        return "Domain{" +
                "person=" + person +
                ", sslCertificate=" + sslCertificate +
                ", personId='" + personId + '\'' +
                ", regSpec=" + regSpec +
                ", dnsResourceRecords=" + dnsResourceRecords +
                ", sslCertificateId='" + sslCertificateId + '\'' +
                ", autoRenew=" + autoRenew +
                ", parentDomainId='" + parentDomainId + '\'' +
                ", synced='" + synced + '\'' +
                "} " + super.toString();
    }
}
