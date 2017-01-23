package ru.majordomo.hms.rc.user.resources.DAO;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;

import java.util.List;
public interface DNSResourceRecordDAO {
    void update(DNSResourceRecord record);
    void insert(DNSResourceRecord record);
    boolean insertByDomainName(String domainName, DNSResourceRecord record);
    List<DNSResourceRecord> getByDomainNameAndTypeIn(String domainName, List<DNSResourceRecordType> types);
    List<DNSResourceRecord> getByDomainName(String domainName);
}