package ru.majordomo.hms.rc.user.resources;

public class DNSResourceRecord {
    private String ownerName;
    private Long ttl;
    private String data;
    private DNSResourceRecordClass rrClass;
    private DNSResourceRecordType rrType;

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
}
