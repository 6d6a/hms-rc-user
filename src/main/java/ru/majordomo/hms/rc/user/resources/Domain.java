package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "domains")
public class Domain extends Resource {

    @Transient
    private Person person;

    @Transient
    private SSLCertificate sslCertificate;

    private String personId;
    private RegSpec regSpec;
    private List<DNSResourceRecord> dnsResourceRecords = new ArrayList<>();
    private String sslCertificateId;
    private Boolean autoRenew;
    private String parentDomainId;

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

    @Override
    public String toString() {
        return "Domain{" +
                "id=" + getId() +
                ", name=" + getId() +
                ", switchedOn=" + getSwitchedOn() +
                ", person=" + person +
                ", personId='" + personId +
                ", regSpec=" + regSpec +
                ", dnsResourceRecords=" + dnsResourceRecords +
                '}';
    }
}
