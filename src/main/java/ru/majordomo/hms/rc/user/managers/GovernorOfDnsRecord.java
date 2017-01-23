package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAOImpl;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAO;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class GovernorOfDnsRecord extends LordOfResources {

    private Cleaner cleaner;
    private GovernorOfDomain governorOfDomain;

    private DNSDomainDAOImpl dnsDomainDAO;
    private DNSResourceRecordDAOImpl dnsResourceRecordDAO;

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setDnsDomainDAO(DNSDomainDAOImpl dnsDomainDAO) {
        this.dnsDomainDAO = dnsDomainDAO;
    }

    @Autowired
    public void setDnsResourceRecordDAO(DNSResourceRecordDAOImpl dnsResourceRecordDAO) {
        this.dnsResourceRecordDAO = dnsResourceRecordDAO;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {

    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {

    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        throw new NotImplementedException();
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DNSResourceRecord> records = new ArrayList<>();

        if (hasNameAndAccountId(keyValue)) {
            records = dnsResourceRecordDAO.getByDomainName(keyValue.get("name"));
        }

        return records;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return null;
    }

    @Override
    public void store(Resource resource) {

    }

    void validateDomain(Domain domain) {
        String domainName = domain.getName();
        if (!dnsDomainDAO.hasDomainRecord(domainName)) {
            dnsDomainDAO.insert(domainName);
        }
    }

    void addSoaAndNsRecords(Domain domain) {
        dnsResourceRecordDAO.insertByDomainName(domain.getName(), new DNSResourceRecord());
        dnsResourceRecordDAO.insertByDomainName(domain.getName(), new DNSResourceRecord());
        dnsResourceRecordDAO.insertByDomainName(domain.getName(), new DNSResourceRecord());
    }
}
