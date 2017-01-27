package ru.majordomo.hms.rc.user.managers;

import com.mysql.management.util.NotImplementedException;
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
        if (serviceMessage.getParam("resourceId") == null) {
            throw new ParameterValidateException("Необходимо указать resourceId");
        }
        Long recordId = ((Number) serviceMessage.getParam("resourceId")).longValue();
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        record = setRecordParams(serviceMessage, record);
        validate(record);
        store(record);
        return record;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (resourceId == null) {
            throw new ParameterValidateException("Необходимо указать resourceId");
        }
        Long recordId;
        try {
            recordId = Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("ID DNS-записи имеет числовой формат");
        }
        dnsResourceRecordDAO.delete(recordId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        DNSResourceRecord record = new DNSResourceRecord();
        return setRecordParams(serviceMessage, record);
    }

    private DNSResourceRecord setRecordParams(ServiceMessage serviceMessage, DNSResourceRecord record) {
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
                record.setTtl(((Number) serviceMessage.getParam("ttl")).longValue());
            }
            if (serviceMessage.getParam("prio") != null) {
                record.setPrio(((Number) serviceMessage.getParam("prio")).longValue());
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
        DNSResourceRecord record = (DNSResourceRecord) resource;
        if (record.getName() == null || record.getName().equals("")) {
            throw new ParameterValidateException("Имя домена должно быть указано");
        }
    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        DNSResourceRecord record = (DNSResourceRecord) resource;
        record.setRrClass(DNSResourceRecordClass.IN);
        record.setName(dnsResourceRecordDAO.getDomainNameByRecordId(record.getRecordId()));
        return resource;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Long recordId;
        try {
            recordId = Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("ID DNS-записи имеет числовой формат");
        }
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        return construct(record);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
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
