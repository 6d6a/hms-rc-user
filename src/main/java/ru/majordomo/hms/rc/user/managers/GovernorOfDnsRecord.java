package ru.majordomo.hms.rc.user.managers;

import com.mysql.management.util.NotImplementedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.validation.group.DnsRecordChecks;

import java.net.IDN;
import java.util.*;

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
    public DNSResourceRecord create(ServiceMessage serviceMessage) throws ParameterValidationException {
        DNSResourceRecord record = buildResourceFromServiceMessage(serviceMessage);
        preValidate(record);
        validate(record);
        store(record);

        return record;
    }

    @Override
    public DNSResourceRecord update(ServiceMessage serviceMessage) throws ParameterValidationException {
        String resourceId = (String) serviceMessage.getParam("resourceId");
        if (resourceId == null) {
            throw new ParameterValidationException("Необходимо указать resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        DNSResourceRecord record = build(keyValue);

        setRecordParams(record, serviceMessage);

        preValidate(record);
        validate(record);
        store(record);

        return record;
    }

    @Override
    public void preDelete(String resourceId) {

    }

    /**
     * удаляет домен и dns записи из базы данных dns-сервера
     * @param internationalizedDomainName - имя домена, допускаются кириллические символы
     */
    public void dropDomain(String internationalizedDomainName) {
        dnsResourceRecordDAO.dropDomain(IDN.toASCII(internationalizedDomainName));
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (resourceId == null) {
            throw new ParameterValidationException("Необходимо указать resourceId");
        }
        Long recordId;
        try {
            recordId = Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new ParameterValidationException("ID DNS-записи имеет числовой формат");
        }

        preDelete(resourceId);
        dnsResourceRecordDAO.delete(recordId);
    }

    @Override
    public DNSResourceRecord buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        DNSResourceRecord record = new DNSResourceRecord();
        setResourceParams(record, serviceMessage, cleaner);
        setRecordParams(record, serviceMessage);

        return record;
    }

    private void setRecordParams(DNSResourceRecord record, ServiceMessage serviceMessage) {
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
            if (serviceMessage.getParam("prio") != null) {
                if (serviceMessage.getParam("prio") instanceof String) {
                    String prioStr = (String) serviceMessage.getParam("prio");
                    record.setPrio(prioStr.isEmpty() ? null : Long.parseLong(prioStr));
                } else {
                    record.setPrio(((Number) serviceMessage.getParam("prio")).longValue());
                }
            }
            if (serviceMessage.getParam("type") != null) {
                for (DNSResourceRecordType type : DNSResourceRecordType.values()) {
                    if (serviceMessage.getParam("type").equals(type.name())) {
                        record.setRrType(type);
                    }
                }
            }
        } catch (ClassCastException | NumberFormatException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }
    }

    @Override
    public void preValidate(DNSResourceRecord record) {
        DNSResourceRecordType type = record.getRrType();
        
        if(type != null) {
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
    }

    @Override
    public void validate(DNSResourceRecord record) throws ParameterValidationException {
        Set<ConstraintViolation<DNSResourceRecord>> constraintViolations = validator.validate(record, DnsRecordChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("record: " + record + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected DNSResourceRecord construct(DNSResourceRecord record) throws ParameterValidationException {
        record.setRrClass(DNSResourceRecordClass.IN);
        record.setName(dnsResourceRecordDAO.getDomainNameByRecordId(record.getRecordId()));
        record.setOwnerName(IDN.toUnicode(record.getOwnerName()));
        return record;
    }

    private List<DNSResourceRecord> constructByDomain(List<DNSResourceRecord> records) throws ParameterValidationException {
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
            throw new ParameterValidationException("ID DNS-записи должен быть в числовом формате");
        }
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);
        return construct(record);
    }

    @Override
    public DNSResourceRecord build(Map<String, String> keyValue) throws ResourceNotFoundException {
        if (keyValue.get("resourceId") == null) {
            throw new ResourceNotFoundException("Должен быть указан resourceId");
        }

        Long recordId;
        try {
            recordId = Long.parseLong(keyValue.get("resourceId"));
        } catch (NumberFormatException e) {
            throw new ParameterValidationException("ID DNS-записи должен быть в числовом формате");
        }

        DNSResourceRecord record = dnsResourceRecordDAO.findOne(recordId);

        if (hasResourceIdAndAccountId(keyValue)) {
            Map<String, String> domainsKeyValue = new HashMap<>();
            domainsKeyValue.put("name", IDN.toUnicode(record.getName()));
            domainsKeyValue.put("accountId", keyValue.get("accountId"));
            if (governorOfDomain.build(domainsKeyValue) == null) {
                throw new ParameterValidationException("Домен, указанный в ДНС-записи, не принадлежит аккаунту " + record.getAccountId());
            }
            record.setAccountId(keyValue.get("accountId"));
        }

        if (record == null) {
            throw new ResourceNotFoundException("ДНС-запись с ID:" + keyValue.get("resourceId") + " не найдена");
        }

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

    @Transactional
    public void initDomain(Domain domain) {
        String domainName = IDN.toASCII(domain.getName());
        Long domainId = dnsResourceRecordDAO.initDomain(domainName);
        addSoaRecord(domain, domainId);
        addNsRecords(domain, domainId);
        addDefaultARecords(domain, domainId);
        addMailRecords(domain, domainId);
        if (domain.getDkim() != null) {
            setupDkimRecords(domain);
        }
    }

    public void addNsRecords(Domain domain, Long domainId) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord record = new DNSResourceRecord();
        record.setDomainId(domainId);
        record.setRrType(DNSResourceRecordType.NS);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        record.setData("ns.majordomo.ru");
        dnsResourceRecordDAO.insert(record);
        record.setData("ns2.majordomo.ru");
        dnsResourceRecordDAO.insert(record);
        record.setData("ns3.majordomo.ru");
        dnsResourceRecordDAO.insert(record);
        record.setData("ns4.majordomo.ru");
        dnsResourceRecordDAO.insert(record);
    }

    public void addARecords(Domain domain) {
        String ownerDomainName = IDN.toASCII(domain.getName());

        String domainName;
        if (domain.getParentDomainId() != null) {
            try {
                Domain parentDomain = governorOfDomain.build(domain.getParentDomainId());
                domainName = IDN.toASCII(parentDomain.getName());
            } catch (ResourceNotFoundException e) {
                log.error("[addARecords] Ошибка: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            domainName = ownerDomainName;
        }
        DNSResourceRecord record = new DNSResourceRecord();
        record.setRrType(DNSResourceRecordType.A);
        record.setOwnerName(ownerDomainName);
        record.setTtl(3600L);
        try {
            record.setData(getServerPrimaryIp(domain.getAccountId()));
        } catch (ParameterValidationException e) {
            log.error("[addARecords] Ошибка: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
        record.setOwnerName("*." + ownerDomainName);
        dnsResourceRecordDAO.insertByDomainName(domainName, record);
    }

    public void addSoaRecord(Domain domain, Long domainId) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord record = new DNSResourceRecord();
        record.setDomainId(domainId);
        record.setRrType(DNSResourceRecordType.SOA);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        record.setData("ns.majordomo.ru. support.majordomo.ru. 2004032900 3600 900 3600000 3600");
        dnsResourceRecordDAO.insert(record);
    }

    public void addDefaultARecords(Domain domain, Long domainId) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord record = new DNSResourceRecord();
        record.setDomainId(domainId);
        record.setRrType(DNSResourceRecordType.A);
        record.setOwnerName(domainName);
        record.setTtl(3600L);
        try {
            record.setData(getServerPrimaryIp(domain.getAccountId()));
        } catch (ParameterValidationException e) {
            log.error("[addDefaultARecords] Ошибка: " + e.getMessage());
            return;
        }
        dnsResourceRecordDAO.insert(record);
        record.setOwnerName("*." + domainName);
        dnsResourceRecordDAO.insert(record);
    }

    private String getServerPrimaryIp(String accountId) {
        String serverId;
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        Collection<UnixAccount> unixAccounts = governorOfUnixAccount.buildAll(keyValue);
        if (unixAccounts.iterator().hasNext()) {
            serverId = unixAccounts.iterator().next().getServerId();
        } else {
            throw new ParameterValidationException("Не удалось получить ID сервера");
        }
        Map<String, String> serverIpInfo = rcStaffClient.getServerIpInfoByServerId(serverId);
        if (serverIpInfo != null) {
            return serverIpInfo.get("primaryIp");
        } else {
            throw new ParameterValidationException("Не удалось получить IP сервера");
        }
    }

    public void addMailRecords(Domain domain, Long domainId) {
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord mxRecord = new DNSResourceRecord();
        mxRecord.setDomainId(domainId);
        mxRecord.setRrType(DNSResourceRecordType.MX);
        mxRecord.setRrClass(DNSResourceRecordClass.IN);
        mxRecord.setTtl(3600L);
        mxRecord.setOwnerName(domainName);
        mxRecord.setData("mmxs.majordomo.ru");
        mxRecord.setPrio(10L);
        dnsResourceRecordDAO.insert(mxRecord);

        DNSResourceRecord cnameRecord = new DNSResourceRecord();
        cnameRecord.setDomainId(domainId);
        cnameRecord.setRrClass(DNSResourceRecordClass.IN);
        cnameRecord.setRrType(DNSResourceRecordType.CNAME);
        cnameRecord.setTtl(3600L);
        cnameRecord.setOwnerName("smtp." + domainName);
        cnameRecord.setData("smtp.majordomo.ru");
        dnsResourceRecordDAO.insert(cnameRecord);

        cnameRecord.setOwnerName("pop3." + domainName);
        cnameRecord.setData("pop3.majordomo.ru");
        dnsResourceRecordDAO.insert(cnameRecord);

        cnameRecord.setOwnerName("mail." + domainName);
        cnameRecord.setData("mail.majordomo.ru");
        dnsResourceRecordDAO.insert(cnameRecord);
    }
    
    public void setupDkimRecords(Domain domain) {
        if (domain.getDkim() == null || domain.getDkim().getData() == null) return;
        String domainName = IDN.toASCII(domain.getName());

        DNSResourceRecord dkimRecord = new DNSResourceRecord();
        dkimRecord.setRrType(DNSResourceRecordType.TXT);
        dkimRecord.setRrClass(DNSResourceRecordClass.IN);
        dkimRecord.setTtl(3600L);
        dkimRecord.setOwnerName(domain.getDkim().getSelector() + "._domainkey." + domainName);
        dkimRecord.setData(domain.getDkim().getData());

        dnsResourceRecordDAO.insertOrUpdateByOwnerName(domainName, dkimRecord);
    }

    void setZoneStatus(Domain domain, Boolean switchedOn) {
        dnsResourceRecordDAO.switchByDomainName(IDN.toASCII(domain.getName()), switchedOn);
    }
}
