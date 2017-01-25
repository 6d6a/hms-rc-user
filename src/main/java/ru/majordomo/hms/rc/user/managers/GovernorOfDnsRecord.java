package ru.majordomo.hms.rc.user.managers;

import com.mysql.management.util.Str;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAOImpl;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class GovernorOfDnsRecord extends LordOfResources {

    private Cleaner cleaner;
    private GovernorOfDomain governorOfDomain;

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
    public void setDnsResourceRecordDAO(DNSResourceRecordDAOImpl dnsResourceRecordDAO) {
        this.dnsResourceRecordDAO = dnsResourceRecordDAO;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        DNSResourceRecord record = (DNSResourceRecord) buildResourceFromServiceMessage(serviceMessage);
        validate(record);
        store(record);

        return record;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException {
        Long recordId = (Long) serviceMessage.getParam("recordId");
        if (recordId == null) {
            throw new ParameterValidateException("Необходимо указать recordId");
        }
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        try {
            if (serviceMessage.getParam("ownerName") != null) {
                record.setOwnerName(cleaner.cleanString((String) serviceMessage.getParam("ownerName")));
            }
            if (serviceMessage.getParam("data") != null) {
                record.setData(cleaner.cleanString((String) serviceMessage.getParam("data")));
            }
            if (serviceMessage.getParam("ttl") != null) {
                record.setTtl((Long) serviceMessage.getParam("ttl"));
            }
            if (serviceMessage.getParam("prio") != null) {
                record.setPrio((Long) serviceMessage.getParam("prio"));
            }
            if (serviceMessage.getParam("type") != null) {
                for (DNSResourceRecordType type : DNSResourceRecordType.values()) {
                    if (serviceMessage.getParam("type").equals(type.name())) {
                        record.setRrType(type);
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }
        validate(record);
        store(record);
        return record;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        Long recordId = Long.parseLong(resourceId);
        dnsResourceRecordDAO.delete(recordId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        DNSResourceRecord record = new DNSResourceRecord();
        try {
            if (serviceMessage.getParam("name") != null) {
                record.setName(cleaner.cleanString((String) serviceMessage.getParam("name")));
            }
            if (serviceMessage.getParam("ownerName") != null) {
                record.setOwnerName(cleaner.cleanString((String) serviceMessage.getParam("ownerName")));
            }
            if (serviceMessage.getParam("data") != null) {
                record.setData(cleaner.cleanString((String) serviceMessage.getParam("data")));
            }
            if (serviceMessage.getParam("ttl") != null) {
                record.setTtl((Long) serviceMessage.getParam("ttl"));
            }
            if (serviceMessage.getParam("prio") != null) {
                record.setPrio((Long) serviceMessage.getParam("prio"));
            }
            if (serviceMessage.getParam("type") != null) {
                for (DNSResourceRecordType type : DNSResourceRecordType.values()) {
                    if (serviceMessage.getParam("type").equals(type.name())) {
                        record.setRrType(type);
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        return record;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {

    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        DNSResourceRecord record = (DNSResourceRecord) resource;
        record.setRrClass(DNSResourceRecordClass.IN);
        record.setName(dnsResourceRecordDAO.getDomainNameByRecordId(record.getDomainId()));
        return resource;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Long recordId = Long.parseLong(resourceId);
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        if (record == null) {
            throw new ResourceNotFoundException("Не найдено DNS-записи с ID " + resourceId);
        }
        return construct(record);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DNSResourceRecord> records = new ArrayList<>();

        List<DNSResourceRecord> preRecords = new ArrayList<>();
        if (hasNameAndAccountId(keyValue)) {
            preRecords = dnsResourceRecordDAO.getByDomainName(keyValue.get("name"));
        }

        if (preRecords.size() > 0) {
            for (DNSResourceRecord record : preRecords) {
                records.add((DNSResourceRecord) construct(record));
            }
        }

        return records;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return null;
    }

    @Override
    public void store(Resource resource) {
        dnsResourceRecordDAO.save((DNSResourceRecord) resource);
    }

    public void initDomain(Domain domain) {
        String domainName = domain.getName();
        dnsResourceRecordDAO.initDomain(domainName);
        addSoaRecord(domain);
        addNsRecords(domain);
    }

    public void addNsRecords(Domain domain) {
        String domainName = domain.getName();

        DNSResourceRecord record = new DNSResourceRecord();
        record.setRrType(DNSResourceRecordType.NS);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        record.setData("ns.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
        record.setData("ns2.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
        record.setData("ns3.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
    }

    public void addSoaRecord(Domain domain) {
        String domainName = domain.getName();

        DNSResourceRecord record = new DNSResourceRecord();
        record.setRrType(DNSResourceRecordType.SOA);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        record.setData("ns.majordomo.ru. support.majordomo.ru. 2004032900 3600 900 3600000 3600");
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
    }
}
