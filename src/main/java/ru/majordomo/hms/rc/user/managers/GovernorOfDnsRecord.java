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
import java.util.*;

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
        String resourceId = (String) serviceMessage.getParam("resourceId");
        Long recordId;
        try {
            recordId = Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("ID DNS-записи имеет числовой формат");
        }
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        record = setRecordParams(serviceMessage, record);
        validate(record);
        store(record);
        return record;
    }

    @Override
    public void preDelete(String resourceId) {

    }

    public void dropDomain(String domainName) {
        dnsResourceRecordDAO.dropDomain(domainName);
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

        preDelete(resourceId);
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
            if (serviceMessage.getParam("ttl") != null && !serviceMessage.getParam("ttl").equals("")) {
                record.setTtl(((Number) serviceMessage.getParam("ttl")).longValue());
            }
            if (serviceMessage.getParam("prio") != null && !serviceMessage.getParam("prio").equals("")) {
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
        if (!record.getOwnerName().endsWith(record.getName())) {
            throw new ParameterValidateException("Запись должна кончаться именем домена");
        }
        if (record.getRrType() == null) {
            throw new ParameterValidateException("Тип записи должен быть указан");
        }
        if (record.getTtl() == null) {
            record.setTtl(3600L);
        }
        DNSResourceRecordType type = record.getRrType();
        switch (type) {
            case A:
                record.setPrio(null);
                break;
            case MX:
                if (record.getPrio() == null) record.setPrio(10L);
                break;
            case AAAA:
                break;
            default:
                break;
        }
//        Map<String, String> keyValue = new HashMap<>();
//        keyValue.put("name", record.getName());
//        keyValue.put("accountId", record.getAccountId());
//        if (governorOfDomain.build(keyValue) == null) {
//            throw new ParameterValidateException("Домен, для которого создаётся запись, не принадлежит аккаунту " + record.getAccountId());
//        }
    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        DNSResourceRecord record = (DNSResourceRecord) resource;
        record.setRrClass(DNSResourceRecordClass.IN);
        record.setName(dnsResourceRecordDAO.getDomainNameByRecordId(record.getRecordId()));
        return resource;
    }

    private List<DNSResourceRecord> constructByDomain(List<DNSResourceRecord> records) throws ParameterValidateException {
        if (records.size() > 0) {
            String domainName = dnsResourceRecordDAO.getDomainNameByRecordId(records.get(0).getRecordId());
            for (DNSResourceRecord record : records) {
                record.setRrClass(DNSResourceRecordClass.IN);
                record.setName(domainName);
            }
        }

        return records;
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
        if (keyValue.get("resourceId") == null) {
            throw new ResourceNotFoundException("Должен быть указан resourceId");
        }
        Long recordId;
        try {
            recordId = Long.parseLong(keyValue.get("resourceId"));
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("ID DNS-записи имеет числовой формат");
        }
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        return construct(record);
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DNSResourceRecord> records = new ArrayList<>();

        if (hasNameAndAccountId(keyValue)) {
            records = dnsResourceRecordDAO.getByDomainName(keyValue.get("name"));
        }

        return constructByDomain(records);
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
