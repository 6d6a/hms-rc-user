package ru.majordomo.hms.rc.user.resources.DAO;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;

import java.util.List;
public interface DNSResourceRecordDAO {
    void update(DNSResourceRecord record);
    void insert(DNSResourceRecord record);
    void delete(DNSResourceRecord record);
    void delete(Long recordId);
    DNSResourceRecord findOne(Long recordId);
    boolean insertByDomainName(String domainName, DNSResourceRecord record);
    List<DNSResourceRecord> getByDomainNameAndTypeIn(String domainName, List<DNSResourceRecordType> types);
    List<DNSResourceRecord> getByDomainName(String domainName);
    List<DNSResourceRecord> getNSRecords(String domainName);
    String getDomainNameByRecordId(Long recordId);
    void initDomain(String domainName);
}