package ru.majordomo.hms.rc.user.resources;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.validation.ValidDnsRecord;

@ValidDnsRecord
public class DNSResourceRecord extends Resource {
    private Long domainId;
    private Long recordId;
    private String ownerName;
    private Long ttl = 3600L;
    private String data;
    private Long prio;
    private DNSResourceRecordClass rrClass;

    @NotNull(message = "Тип записи должен быть указан")
    private DNSResourceRecordType rrType;

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getPrio() {
        return prio;
    }

    public void setPrio(Long prio) {
        this.prio = prio;
    }

    public DNSResourceRecordClass getRrClass() {
        return rrClass;
    }

    public void setRrClass(DNSResourceRecordClass rrClass) {
        this.rrClass = rrClass;
    }

    public DNSResourceRecordType getRrType() {
        return rrType;
    }

    public void setRrType(DNSResourceRecordType rrType) {
        this.rrType = rrType;
    }

    @Override
    public void switchResource() {

    }

    @Override
    public String toString() {
        return "DNSRecord for domain with id " + domainId + ":{" +
                "id=" + this.getId() +
                ", recordId=" + recordId +
                ", name='" + this.getName() + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", type=" + rrType +
                ", content='" + data + '\'' +
                ", ttl=" + ttl +
                ", priority=" + prio + 
                '}';
    }
}
