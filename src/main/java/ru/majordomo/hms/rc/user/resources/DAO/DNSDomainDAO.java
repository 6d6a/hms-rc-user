package ru.majordomo.hms.rc.user.resources.DAO;

public interface DNSDomainDAO {
    void insert(String domainName);
    void update(String domainName);
    void delete(String domainName);
    void switchDomain(String domainName);
    Boolean hasDomainRecord(String domainName);
}
