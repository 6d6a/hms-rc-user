package ru.majordomo.hms.rc.user.managers;

import com.google.common.net.InternetDomainName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Service
public class GovernorOfDomain extends LordOfResources {

    private Cleaner cleaner;
    private DomainRepository repository;
    private GovernorOfPerson governorOfPerson;
    private GovernorOfSSLCertificate governorOfSSLCertificate;
    private GovernorOfDnsRecord governorOfDnsRecord;
    private GovernorOfMailbox governorOfMailbox;
    private GovernorOfWebSite governorOfWebSite;
    private DomainRegistrarClient registrar;


    @Autowired
    public void setGovernorOfPerson(GovernorOfPerson governorOfPerson) {
        this.governorOfPerson = governorOfPerson;
    }

    @Autowired
    public void setGovernorOfSSLCertificate(GovernorOfSSLCertificate governorOfSSLCertificate) {
        this.governorOfSSLCertificate = governorOfSSLCertificate;
    }

    @Autowired
    public void setGovernorOfDnsRecord(GovernorOfDnsRecord governorOfDnsRecord) {
        this.governorOfDnsRecord = governorOfDnsRecord;
    }

    @Autowired
    public void setGovernorOfMailbox(GovernorOfMailbox governorOfMailbox) {
        this.governorOfMailbox = governorOfMailbox;
    }

    @Autowired
    public void setGovernorOfWebSite(GovernorOfWebSite governorOfWebSite) {
        this.governorOfWebSite = governorOfWebSite;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setRepository(DomainRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setRegistrar(DomainRegistrarClient registrar) {
        this.registrar = registrar;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Domain domain;
        try {
            Boolean needRegister = null;
            if (serviceMessage.getParam("register") != null) {
                needRegister = (Boolean) serviceMessage.getParam("register");
            }

            domain = (Domain) buildResourceFromServiceMessage(serviceMessage);
            validate(domain);

            if (repository.findByName(domain.getName()) != null) {
                throw new ParameterValidateException("Домен " + domain.getName() + " уже присутствует в системе");
            }

            if (needRegister != null && needRegister) {
                try {
                    registrar.registerDomain(domain.getPerson().getNicHandle(), domain.getName());
                    domain.setRegSpec(registrar.getRegSpec(domain.getName()));
                } catch (Exception e) {
                    throw new ParameterValidateException(e.getMessage());
                }
            }
            store(domain);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        governorOfDnsRecord.initDomain(domain);

        return domain;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidateException("Не указан resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Domain domain = (Domain) build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "autoRenew":
                        domain.setAutoRenew((Boolean) entry.getValue());
                        break;
                    case "renew":
                        if ((Boolean) entry.getValue()) {
                            try {
                                if (domain.getPersonId() == null) {
                                    throw new ParameterValidateException("Отсутствует personId");
                                }
                                Person person = (Person) governorOfPerson.build(domain.getPersonId());
                                ResponseEntity responseEntity = registrar.renewDomain(person.getNicHandle(), domain.getName());
                                if (!responseEntity.getStatusCode().equals(HttpStatus.CREATED)) {
                                    throw new ParameterValidateException("Ошибка при продлении домена");
                                }
                                domain.setRegSpec(registrar.getRegSpec(domain.getName()));
                            } catch (Exception e) {
                                throw new ParameterValidateException(e.getMessage());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validate(domain);
        store(domain);

        return domain;
    }

    @Override
    public void preDelete(String resourceId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("domainId", resourceId);

        WebSite webSite;
        try {
            webSite = (WebSite) governorOfWebSite.build(keyValue);
            if (webSite != null) {
                throw new ParameterValidateException("Домен используется в вебсайте с ID " + webSite.getId());
            }
        } catch (ResourceNotFoundException e) {
            logger.debug("Вебсайтов использующих домен с ID " + resourceId + " не обнаружено");
        }

        List<Mailbox> mailboxes = (List<Mailbox>) governorOfMailbox.buildAll(keyValue);
        if (mailboxes.size() > 0) {
            String message = "На домене имеются почтовые ящики: ";

            for (Mailbox mailbox : mailboxes) {
                message += mailbox.getFullName() + ", ";
            }
            throw new ParameterValidateException(message.substring(0, message.length() - 2));
        }

        Domain domain = repository.findOne(resourceId);
        governorOfDnsRecord.dropDomain(domain.getName());
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Domain domain = new Domain();
        LordOfResources.setResourceParams(domain, serviceMessage, cleaner);
        if (serviceMessage.getParam("register") != null && (Boolean) serviceMessage.getParam("register")) {
            if (serviceMessage.getParam("personId") != null) {
                String domainPersonId = cleaner.cleanString((String) serviceMessage.getParam("personId"));
                Person domainPerson = (Person) governorOfPerson.build(domainPersonId);
                domain.setPerson(domainPerson);
                governorOfPerson.validate(domainPerson);
            } else {
                throw new ParameterValidateException("Персона должна быть указана");
            }
        }
        return domain;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Domain domain = (Domain) resource;
        if (domain.getName() == null || domain.getName().equals("")) {
            throw new ParameterValidateException("Имя домена не может быть пустым");
        }
        validateDomainName(domain.getName());

        Person domainPerson = domain.getPerson();
        if (domainPerson != null) {
            governorOfPerson.validate(domainPerson);
        }

        if (domain.getAutoRenew() == null) {
            domain.setAutoRenew(false);
        }

        SSLCertificate sslCertificate = domain.getSslCertificate();
        if (sslCertificate != null) {
            governorOfSSLCertificate.validate(sslCertificate);
        }
    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        Domain domain = (Domain) resource;

        if (domain.getPersonId() != null) {
            Person domainPerson = (Person) governorOfPerson.build(domain.getPersonId());
            domain.setPerson(domainPerson);
        }

        if (domain.getSslCertificateId() != null) {
            SSLCertificate sslCertificate = (SSLCertificate) governorOfSSLCertificate.build(domain.getSslCertificateId());
            domain.setSslCertificate(sslCertificate);
        } else {
            domain.setSslCertificate(null);
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domain.getName());
        keyValue.put("accountId", domain.getAccountId());
        domain.setDnsResourceRecords((List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue));

        return domain;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Domain domain = repository.findOne(resourceId);
        if (domain == null) {
            throw new ResourceNotFoundException("Domain с ID:" + resourceId + " не найден");
        }
        return construct(domain);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Domain domain = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            domain = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (hasNameAndAccountId(keyValue)) {
            domain = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
        } else if (keyValue.get("name") != null) {
            domain = repository.findByName(keyValue.get("name"));
        }

        if (domain == null) {
            throw new ResourceNotFoundException("Не удалось найти указанный домен");
        }

        return construct(domain);
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Domain> buildedDomains = new ArrayList<>();

        boolean byAccountId = false;
        boolean byExpiringDates = false;
        boolean byPersonId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }

            if (hasPaidTillDates(keyValue)) {
                byExpiringDates = true;
            }

            if (entry.getKey().equals("personId")) {
                byPersonId = true;
            }
        }

        if (byAccountId && byExpiringDates) {
            try {
                LocalDate startDate = LocalDate.parse(keyValue.get("paidTillStart"));
                LocalDate endDate = LocalDate.parse(keyValue.get("paidTillEnd"));
                for (Domain domain : repository.findByAccountIdAndRegSpecPaidTillBetween(keyValue.get("accountId"), startDate, endDate)) {
                    buildedDomains.add((Domain) construct(domain));
                }
            } catch (DateTimeParseException e) {
                throw new ParameterValidateException("Одна из дат указана неверно");
            }
        } else if (byAccountId) {
            for (Domain domain : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedDomains.add((Domain) construct(domain));
            }
        } else if (byExpiringDates) {
            try {
                LocalDate startDate = LocalDate.parse(keyValue.get("paidTillStart"));
                LocalDate endDate = LocalDate.parse(keyValue.get("paidTillEnd"));
                for (Domain domain : repository.findByRegSpecPaidTillBetween(startDate, endDate)) {
                    buildedDomains.add((Domain) construct(domain));
                }
            } catch (DateTimeParseException e) {
                throw new ParameterValidateException("Одна из дат указана неверно");
            }
        } else if (byPersonId) {
            for (Domain domain : repository.findByPersonId(keyValue.get("personId"))) {
                buildedDomains.add((Domain) construct(domain));
            }
        }

        return buildedDomains;
    }

    private boolean hasPaidTillDates(Map<String, String> keyValue) {
        boolean hasPaidTillStart = false;
        boolean hasPaidTillEnd = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("paidTillStart")) {
                hasPaidTillStart = true;
            }
            if (entry.getKey().equals("paidTillStart")) {
                hasPaidTillEnd = true;
            }
        }

        return hasPaidTillStart && hasPaidTillEnd;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<Domain> buildedDomains = new ArrayList<>();
        for (Domain domain : repository.findAll()) {
            buildedDomains.add((Domain) construct(domain));
        }
        return buildedDomains;
    }

    @Override
    public void store(Resource resource) {
        Domain domain = (Domain) resource;
        repository.save(domain);
    }

    private void validateDomainName(String domainName) throws ParameterValidateException {
        try {
            InternetDomainName domain = InternetDomainName.from(domainName);
            String domainPublicPart = domain.publicSuffix().toString();
        } catch (Exception e) {
            throw new ParameterValidateException("Некорректное имя домена");
        }
    }
}
