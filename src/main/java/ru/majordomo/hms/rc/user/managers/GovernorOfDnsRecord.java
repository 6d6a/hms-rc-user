package ru.majordomo.hms.rc.user.managers;

import com.mysql.management.util.NotImplementedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.validation.group.DnsRecordChecks;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

@Service
public class GovernorOfDnsRecord extends LordOfResources<DNSResourceRecord> {

    private Cleaner cleaner;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Validator validator;
    private DNSResourceRecordDAOImpl dnsResourceRecordDAO;
    private StaffResourceControllerClient rcStaffClient;

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfUnixAccount governorOfUnixAccount) {
        this.governorOfUnixAccount = governorOfUnixAccount;
    }

    @Autowired
    public void setDnsResourceRecordDAO(DNSResourceRecordDAOImpl dnsResourceRecordDAO) {
        this.dnsResourceRecordDAO = dnsResourceRecordDAO;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Autowired
    public void setRcStaffClient(StaffResourceControllerClient rcStaffClient) {
        this.rcStaffClient = rcStaffClient;
    }

    @Override
    public DNSResourceRecord create(ServiceMessage serviceMessage) throws ParameterValidateException {
        DNSResourceRecord record = buildResourceFromServiceMessage(serviceMessage);
        validate(record);
        store(record);

        return record;
    }

    @Override
    public DNSResourceRecord update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException {
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
    public DNSResourceRecord buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        DNSResourceRecord record = new DNSResourceRecord();
        return setRecordParams(serviceMessage, record);
    }

    private DNSResourceRecord setRecordParams(ServiceMessage serviceMessage, DNSResourceRecord record) {
        try {
            if (serviceMessage.getParam("name") != null) {
                record.setName(IDN.toASCII(cleaner.cleanString((String) serviceMessage.getParam("name"))));
            }
            if (serviceMessage.getParam("ownerName") != null) {
                record.setOwnerName(IDN.toASCII(cleaner.cleanString((String) serviceMessage.getParam("ownerName"))));
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
    public void preValidate(DNSResourceRecord record) {
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
    }

    @Override
    public void validate(DNSResourceRecord record) throws ParameterValidateException {
        Set<ConstraintViolation<DNSResourceRecord>> constraintViolations = validator.validate(record, DnsRecordChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("record: " + record + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }

//        Map<String, String> keyValue = new HashMap<>();
//        keyValue.put("name", record.getName());
//        keyValue.put("accountId", record.getAccountId());
//        if (governorOfDomain.build(keyValue) == null) {
//            throw new ParameterValidateException("Домен, для которого создаётся запись, не принадлежит аккаунту " + record.getAccountId());
//        }
    }

    @Override
    protected DNSResourceRecord construct(DNSResourceRecord record) throws ParameterValidateException {
        record.setRrClass(DNSResourceRecordClass.IN);
        record.setName(dnsResourceRecordDAO.getDomainNameByRecordId(record.getRecordId()));
        record.setOwnerName(IDN.toUnicode(record.getOwnerName()));
        return record;
    }

    private List<DNSResourceRecord> constructByDomain(List<DNSResourceRecord> records) throws ParameterValidateException {
        if (records.size() > 0) {
            String domainName = dnsResourceRecordDAO.getDomainNameByRecordId(records.get(0).getRecordId());
            for (DNSResourceRecord record : records) {
                record.setRrClass(DNSResourceRecordClass.IN);
                record.setName(domainName);
                record.setOwnerName(IDN.toUnicode(record.getOwnerName()));
            }
        }

        return records;
    }

    @Override
    public DNSResourceRecord build(String resourceId) throws ResourceNotFoundException {
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
    public DNSResourceRecord build(Map<String, String> keyValue) throws ResourceNotFoundException {
        if (keyValue.get("resourceId") == null) {
            throw new ResourceNotFoundException("Должен быть указан resourceId");
        }

        DNSResourceRecord record = build(keyValue.get("resourceId"));

        return construct(record);
    }

    @Override
    public Collection<DNSResourceRecord> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DNSResourceRecord> records = new ArrayList<>();

        if (hasNameAndAccountId(keyValue)) {
            records = dnsResourceRecordDAO.getByDomainName(IDN.toASCII(keyValue.get("name")));
        }

        return constructByDomain(records);
    }

    @Override
    public Collection<DNSResourceRecord> buildAll() {
        throw new NotImplementedException();
    }

    @Override
    public void store(DNSResourceRecord record) {
        dnsResourceRecordDAO.save(record);
    }

    public void initDomain(Domain domain) {
        String domainName = IDN.toASCII(domain.getName());
        dnsResourceRecordDAO.initDomain(domainName);
        addSoaRecord(domain);
        addNsRecords(domain);
        addDefaultARecords(domain);
        addMailRecords(domain);
    }

    public void addNsRecords(Domain domain) {
        String domainName = IDN.toASCII(domain.getName());

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
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord record = new DNSResourceRecord();
        record.setRrType(DNSResourceRecordType.SOA);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        record.setData("ns.majordomo.ru. support.majordomo.ru. 2004032900 3600 900 3600000 3600");
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
    }

    public void addDefaultARecords(Domain domain) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord record = new DNSResourceRecord();
        record.setRrType(DNSResourceRecordType.A);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        try {
            record.setData(getServerPrimaryIp(domain.getAccountId()));
        } catch (ParameterValidateException e) {
            logger.error("[addDefaultARecords] Ошибка: " + e.getMessage());
            return;
        }
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
        record.setOwnerName("*." + domainName);
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
    }

    private String getServerPrimaryIp(String accountId) {
        String serverId;
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        Collection<UnixAccount> unixAccounts = governorOfUnixAccount.buildAll(keyValue);
        if (unixAccounts.iterator().hasNext()) {
            serverId = unixAccounts.iterator().next().getServerId();
        } else {
            throw new ParameterValidateException("Не удалось получить ID сервера");
        }
        Map<String, String> serverIpInfo = rcStaffClient.getServerIpInfoByServerId(serverId);
        if (serverIpInfo != null) {
            return serverIpInfo.get("primaryIp");
        } else {
            throw new ParameterValidateException("Не удалось получить IP сервера");
        }
    }

    public void addMailRecords(Domain domain) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord mxRecord = new DNSResourceRecord();
        mxRecord.setRrType(DNSResourceRecordType.MX);
        mxRecord.setRrClass(DNSResourceRecordClass.IN);
        mxRecord.setTtl(3600L);
        mxRecord.setOwnerName(domainName);
        mxRecord.setData("mmxs.majordomo.ru");
        mxRecord.setPrio(10L);
        dnsResourceRecordDAO.insertByDomainName(domainName, mxRecord);

        DNSResourceRecord cnameRecord = new DNSResourceRecord();
        cnameRecord.setRrClass(DNSResourceRecordClass.IN);
        cnameRecord.setRrType(DNSResourceRecordType.CNAME);
        cnameRecord.setTtl(3600L);
        cnameRecord.setOwnerName("smtp." + domainName);
        cnameRecord.setData("smtp.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, cnameRecord);

        cnameRecord.setOwnerName("pop3." + domainName);
        cnameRecord.setData("pop3.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, cnameRecord);

        cnameRecord.setOwnerName("mail." + domainName);
        cnameRecord.setData("mail.majordomo.ru");
        dnsResourceRecordDAO.insertByDomainName(domainName, cnameRecord);
    }

    void setZoneStatus(Domain domain, Boolean switchedOn) {
        dnsResourceRecordDAO.switchByDomainName(IDN.toASCII(domain.getName()), switchedOn);
    }
}
