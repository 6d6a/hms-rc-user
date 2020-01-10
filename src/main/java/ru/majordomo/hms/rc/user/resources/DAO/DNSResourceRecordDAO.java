package ru.majordomo.hms.rc.user.resources.DAO;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;

import javax.annotation.Nullable;
import java.util.List;
public interface DNSResourceRecordDAO {
    void update(DNSResourceRecord record);
    Long insert(DNSResourceRecord record);
    void delete(DNSResourceRecord record);
    void delete(Long recordId);
    DNSResourceRecord findOne(Long recordId);
    boolean insertByDomainName(String domainName, DNSResourceRecord record);
    List<DNSResourceRecord> getByDomainNameAndTypeIn(String domainName, List<DNSResourceRecordType> types);
    List<DNSResourceRecord> getByDomainName(String domainName);
    List<DNSResourceRecord> getNSRecords(String domainName);
    String getDomainNameByRecordId(Long recordId);
    /**
     * Создает основную DNS-запись домена. Если домен уже создан, вернет его ид и удалит старые записи
     * @param domainName - имя домена в формате punycode
     * @return - вернет Id домена
     */
    @Nullable
    Long initDomain(String domainName);
    void dropDomain(String domainName);
}