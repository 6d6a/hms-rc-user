package ru.majordomo.hms.rc.user.resources;

public class DNSResourceRecord extends Resource {
    private Long pdnsDomainId;
    private Long pdnsRecordId;
    private String ownerName;
    private Long ttl;
    private String data;
    private DNSResourceRecordClass rrClass;
    private DNSResourceRecordType rrType;

    public Long getPdnsDomainId() {
        return pdnsDomainId;
    }

    public void setPdnsDomainId(Long pdnsDomainId) {
        this.pdnsDomainId = pdnsDomainId;
    }

    public Long getPdnsRecordId() {
        return pdnsRecordId;
    }

    public void setPdnsRecordId(Long pdnsRecordId) {
        this.pdnsRecordId = pdnsRecordId;
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
}
