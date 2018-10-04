package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.IDN;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.DTO.StringsContainer;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.validation.group.DomainChecks;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class GovernorOfDomain extends LordOfResources<Domain> {

    private Cleaner cleaner;
    private DomainRepository repository;
    private GovernorOfPerson governorOfPerson;
    private GovernorOfSSLCertificate governorOfSSLCertificate;
    private GovernorOfDnsRecord governorOfDnsRecord;
    private GovernorOfMailbox governorOfMailbox;
    private GovernorOfWebSite governorOfWebSite;
    private GovernorOfRedirect governorOfRedirect;
    private DomainRegistrarClient registrar;
    private Validator validator;
    private MongoOperations mongoOperations;
    private Sender sender;
    private StaffResourceControllerClient staffRcClient;
    protected String applicationName;

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

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Autowired
    public void setGovernorOfRedirect(GovernorOfRedirect governorOfRedirect) {
        this.governorOfRedirect = governorOfRedirect;
    }

    @Autowired
    public void setMongoOperations(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Autowired
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public Domain create(ServiceMessage serviceMessage) throws ParameterValidationException {
        Domain domain;
        try {
            Boolean needRegister = null;
            if (serviceMessage.getParam("register") != null) {
                needRegister = (Boolean) serviceMessage.getParam("register");
            }

            domain = buildResourceFromServiceMessage(serviceMessage);
            validate(domain);

            if (needRegister != null && needRegister) {
                Person person = governorOfPerson.build(domain.getPersonId());
                if (person.getNicHandle() == null || person.getNicHandle().equals("")) {
                    person = governorOfPerson.createPersonRegistrant(person);
                }
                try {
                    registrar.registerDomain(person.getNicHandle(), domain.getName());
                    domain.setRegSpec(registrar.getRegSpec(domain.getName()));
                    domain.setNeedSync(LocalDateTime.now());
                } catch (Exception e) {
                    throw new ParameterValidationException(e.getMessage());
                }
            }
            store(domain);

        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно:" + e.getMessage());
        }

        try {
            if (domain.getParentDomainId() == null)
                governorOfDnsRecord.initDomain(domain);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        return domain;
    }

    @Override
    public Domain update(ServiceMessage serviceMessage) throws ParameterValidationException {
        String resourceId;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidationException("Не указан resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        boolean updateWebSite = false;

        Domain domain = build(keyValue);
        if (domain.getParentDomainId() == null) {
            try {
                for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                    switch (entry.getKey().toString()) {
                        case "autoRenew":
                            domain.setAutoRenew((Boolean) entry.getValue());
                            break;
                        case "renew":
                            if ((Boolean) entry.getValue()) {
                                try {
                                    ResponseEntity responseEntity = registrar.renewDomain(domain.getName(),
                                            domain.getRegSpec().getRegistrar());
                                    if (!responseEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                        throw new ParameterValidationException("Ошибка при продлении домена");
                                    }
                                    RegSpec regSpec = registrar.getRegSpec(domain.getName());
                                    //TODO надо переделать на получении id заявки на продление домена от reg-rpc
                                    // устанавливать статус домена в продление и проверять статус продления
                                    try {
                                        if (regSpec == null) {
                                            if (domain.getRegSpec() != null) {
                                                regSpec = domain.getRegSpec();
                                                regSpec.setPaidTill(domain.getRegSpec().getPaidTill().plusYears(1));
                                                regSpec.setFreeDate(domain.getRegSpec().getFreeDate().plusYears(1));
                                            } else {
                                                regSpec = new RegSpec();
                                                regSpec.setPaidTill(LocalDate.now().plusYears(1));
                                                regSpec.setFreeDate(LocalDate.now().plusYears(1));
                                            }
                                        } else {
                                            if (regSpec.getFreeDate() != null) {
                                                regSpec.setFreeDate(regSpec.getFreeDate().plusYears(1));
                                            } else {
                                                regSpec.setFreeDate(domain.getRegSpec().getFreeDate().plusYears(1));
                                            }
                                            if (regSpec.getPaidTill() != null) {
                                                regSpec.setPaidTill(regSpec.getPaidTill().plusYears(1));
                                            } else {
                                                regSpec.setPaidTill(domain.getRegSpec().getPaidTill().plusYears(1));
                                            }
                                        }
                                    } catch (NullPointerException e) {
                                        //NullPointer может быть в случае, если RegSpec в домене содержит null вместо дат PaidTill или FreeDate
                                    }
                                    domain.setRegSpec(regSpec);
                                    domain.setNeedSync(LocalDateTime.now());
                                } catch (Exception e) {
                                    throw new ParameterValidationException(e.getMessage());
                                }
                            }
                            break;
                        case "switchedOn":
                            Boolean switchedOn = (Boolean) entry.getValue();
                            governorOfDnsRecord.setZoneStatus(domain, switchedOn);

                            domain.setSwitchedOn(switchedOn);

                            updateWebSite = true;

                            break;
                        default:
                            break;
                    }
                }
            } catch (ClassCastException e) {
                throw new ParameterValidationException("Один из параметров указан неверно");
            }
        }

        validate(domain);
        store(domain);

        if (updateWebSite) {
            updateWebSite(domain, serviceMessage);
        }

        return domain;
    }

    private void updateWebSite(Domain domain, ServiceMessage serviceMessage) {
        Map<String, String> webSiteSearch = new HashMap<>();
        webSiteSearch.put("domainId", domain.getId());
        try {
            WebSite webSite = governorOfWebSite.build(webSiteSearch);
            if (webSite != null) {
                ServiceMessage report = new ServiceMessage();
                report.setActionIdentity(serviceMessage.getActionIdentity());
                report.setOperationIdentity(serviceMessage.getOperationIdentity());
                report.setAccountId(serviceMessage.getAccountId());
                report.setObjRef("http://" + applicationName + "/website/" + webSite.getId());
                report.addParam("name", webSite.getName());

                if (report.getAccountId() == null || report.getAccountId().equals("")) {
                    report.setAccountId(webSite.getAccountId());
                }

                report.addParam("success", true);

                String teRoutingKey = getTaskExecutorRoutingKey(webSite);
                sender.send(WEBSITE_UPDATE, teRoutingKey, report);
                webSite.setLocked(true);
                governorOfWebSite.store(webSite);
            }
        } catch (ResourceNotFoundException ignored) {}
    }

    public void setSslCertificateId(Domain domain, String sslCertificateId) {
        Query query = new Query(new Criteria("_id").is(domain.getId()));
        Update update = new Update().set("sslCertificateId", sslCertificateId);
        mongoOperations.updateFirst(query, update, Domain.class);
    }

    public void removeSslCertificateId(SSLCertificate certificate) {
        Domain domain = repository.findBySslCertificateId(certificate.getId());
        if (domain != null) {

            Query query = new Query(new Criteria("sslCertificateId").is(certificate.getId()));
            Update update = new Update().unset("sslCertificateId");
            mongoOperations.updateFirst(query, update, Domain.class);

            updateWebSite(domain);
        }
    }

    private void updateWebSite(Domain domain) {
        updateWebSite(domain, new ServiceMessage());
    }

    public void updateRegSpec(String domainName, RegSpec regSpec) {
        domainName = IDN.toUnicode(domainName.toLowerCase());
        Domain domain = repository.findOneByNameIncludeId(domainName);

        if (domain != null) {
            Query query = new Query(new Criteria("_id").is(domain.getId()));
            Update update = new Update().set("regSpec", regSpec);
            update.currentDate("synced");
            update.unset("needSync");
            mongoOperations.updateFirst(query, update, Domain.class);
        }
    }

    public void clearNotSyncedDomains() {
        Stream<Domain> domainStream = repository.findAllStream();
        domainStream.forEach(domain -> {
            if (domain.getSynced() != null && domain.getRegSpec() != null && domain.getSynced().isBefore(LocalDateTime.now().minusHours(4))) {

                Query query = new Query(new Criteria("_id").is(domain.getId()));
                Update update = new Update().unset("regSpec");
                mongoOperations.updateFirst(query, update, Domain.class);

            } else if (domain.getSynced() == null) {

                Query query = new Query(new Criteria("_id").is(domain.getId()));
                Update update = new Update();
                update.currentDate("synced");
                mongoOperations.updateFirst(query, update, Domain.class);

            }
        });
    }

    @Override
    public void preDelete(String resourceId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("domainId", resourceId);

        List<Domain> subDomains = repository.findByParentDomainId(resourceId);
        if (subDomains.size() > 0) {
            for (Domain subDomain : subDomains) {
                drop(subDomain.getId());
            }
        }

        WebSite webSite;
        try {
            webSite = governorOfWebSite.build(keyValue);
            if (webSite != null) {
                throw new ParameterValidationException("Домен используется в вебсайте с ID " + webSite.getId());
            }
        } catch (ResourceNotFoundException e) {
            logger.debug("Вебсайтов использующих домен с ID " + resourceId + " не обнаружено");
        }

        List<Mailbox> mailboxes = (List<Mailbox>) governorOfMailbox.buildAll(keyValue);
        if (mailboxes.size() > 0) {
            StringBuilder message = new StringBuilder("На домене имеются почтовые ящики: ");

            for (Mailbox mailbox : mailboxes) {
                message.append(mailbox.getFullName()).append(", ");
            }
            throw new ParameterValidationException(message.substring(0, message.length() - 2));
        }

        Redirect redirect;
        try {
            redirect = governorOfRedirect.build(keyValue);
            if (redirect != null) {
                throw new ParameterValidationException("Домен используется в перенаправлении с ID " + redirect.getId());
            }
        } catch (ResourceNotFoundException e) {
            logger.debug("Перенаправлений, использующих домен с ID " + resourceId + " не обнаружено");
        }

        Domain domain = repository.findOne(resourceId);
        if (domain.getParentDomainId() == null) {
            governorOfDnsRecord.dropDomain(domain.getName());
        }
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    public Domain buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Domain domain = new Domain();
        setResourceParams(domain, serviceMessage, cleaner);
        if (serviceMessage.getParam("register") != null && (Boolean) serviceMessage.getParam("register")) {
            if (serviceMessage.getParam("personId") != null) {
                String domainPersonId = cleaner.cleanString((String) serviceMessage.getParam("personId"));
                Person domainPerson = governorOfPerson.build(domainPersonId);
                domain.setPerson(domainPerson);
                governorOfPerson.validate(domainPerson);
            } else {
                throw new ParameterValidationException("Персона должна быть указана");
            }
            
            domain.setAutoRenew(true);
        } else if (serviceMessage.getParam("parentDomainId") != null) {
            Domain parent = repository.findOne((String) serviceMessage.getParam("parentDomainId"));
            if (parent == null) {
                throw new ParameterValidationException("Не найден домен-родитель с id: " + serviceMessage.getParam("parentDomainId"));
            }

            if (parent.getParentDomainId() != null) {
                throw new ParameterValidationException("Домен-родитель не может быть поддоменом");
            }

            domain.setAutoRenew(false);
            domain.setSslCertificateId(null);
            domain.setParentDomainId(parent.getId());
            domain.setName(domain.getName() + "." + parent.getName());
        }
        return domain;
    }

    @Override
    public void validate(Domain domain) throws ParameterValidationException {
        Set<ConstraintViolation<Domain>> constraintViolations = validator.validate(domain, DomainChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("domain: " + domain + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Domain domain) {
        Set<ConstraintViolation<Domain>> constraintViolations = validator.validate(domain, DomainChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("[validateImported] domain: " + domain + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected Domain construct(Domain domain) throws ParameterValidationException {

        if (domain.getPersonId() != null) {
            Person domainPerson = governorOfPerson.build(domain.getPersonId());
            domain.setPerson(domainPerson);
        }

        if (domain.getSslCertificateId() != null) {
            SSLCertificate sslCertificate = governorOfSSLCertificate.build(domain.getSslCertificateId());
            domain.setSslCertificate(sslCertificate);
        } else {
            domain.setSslCertificate(null);
        }

        if (domain.getParentDomainId() != null) {
            Domain parent = repository.findOne(domain.getParentDomainId());
            if (parent == null) {
                throw new ParameterValidationException("Не найден домен-родитель с id: " + domain.getParentDomainId());
            }
            domain.setRegSpec(parent.getRegSpec());
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domain.getName());
        keyValue.put("accountId", domain.getAccountId());
        domain.setDnsResourceRecords((List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue));

        return domain;
    }

    @Override
    public Domain build(String resourceId) throws ResourceNotFoundException {
        Domain domain = repository.findOne(resourceId);
        if (domain == null) {
            throw new ResourceNotFoundException("Domain с ID:" + resourceId + " не найден");
        }
        return construct(domain);
    }

    @Override
    public Domain build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Domain domain = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            domain = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (hasNameAndAccountId(keyValue)) {
            domain = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
        } else if (keyValue.get("name") != null) {
            domain = repository.findByName(keyValue.get("name"));
        } else if (keyValue.get("sslCertificateId") != null) {
            if (keyValue.get("accountId") != null) {
                domain = repository.findBySslCertificateIdAndAccountId(keyValue.get("sslCertificateId"), keyValue.get("accountId"));
            } else {
                domain = repository.findBySslCertificateId(keyValue.get("sslCertificateId"));
            }
        }

        if (domain == null) {
            throw new ResourceNotFoundException("Не удалось найти домен по указанным данным: " + keyValue.toString());
        }

        return construct(domain);
    }

    @Override
    public Collection<Domain> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Domain> buildedDomains = new ArrayList<>();

        boolean byAccountId = false;
        boolean byExpiringDates = false;
        boolean byPersonId = false;
        boolean byParentDomainId = false;
        boolean byNameContains = false;

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

            if (entry.getKey().equals("parentDomainId")) {
                byParentDomainId = true;
            }

            if (entry.getKey().equals("nameContains")) {
                byNameContains = true;
            }
        }

        if (byAccountId && byExpiringDates) {
            try {
                LocalDate startDate = LocalDate.parse(keyValue.get("paidTillStart"));
                LocalDate endDate = LocalDate.parse(keyValue.get("paidTillEnd"));
                for (Domain domain : repository.findByAccountIdAndRegSpecPaidTillBetween(keyValue.get("accountId"), startDate, endDate)) {
                    buildedDomains.add(construct(domain));
                }
            } catch (DateTimeParseException e) {
                throw new ParameterValidationException("Одна из дат указана неверно");
            }
        } else if (byAccountId && byPersonId) {
            for (Domain domain : repository.findByPersonIdAndAccountId(keyValue.get("personId"), keyValue.get("accountId"))) {
                buildedDomains.add(construct(domain));
            }
        } else if (byAccountId && byParentDomainId) {
            for (Domain domain : repository.findByParentDomainIdAndAccountId(keyValue.get("parentDomainId"), keyValue.get("accountId"))) {
                buildedDomains.add(construct(domain));
            }
        } else if (byPersonId) {
            for (Domain domain : repository.findByPersonId(keyValue.get("personId"))) {
                buildedDomains.add(construct(domain));
            }
        } else if (byParentDomainId) {
            for (Domain domain : repository.findByParentDomainId(keyValue.get("parentDomainId"))) {
                buildedDomains.add(construct(domain));
            }
        } else if (byExpiringDates) {
            try {
                LocalDate startDate = LocalDate.parse(keyValue.get("paidTillStart"));
                LocalDate endDate = LocalDate.parse(keyValue.get("paidTillEnd"));
                for (Domain domain : repository.findByRegSpecPaidTillBetween(startDate, endDate)) {
                    buildedDomains.add(construct(domain));
                }
            } catch (DateTimeParseException e) {
                throw new ParameterValidationException("Одна из дат указана неверно");
            }
        } else if (byAccountId) {
            for (Domain domain : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedDomains.add(construct(domain));
            }
        } else if (byNameContains) {
            for (Domain domain : repository.findByNameContains(keyValue.get("nameContains"))) {
                buildedDomains.add(construct(domain));
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
    public Collection<Domain> buildAll() {
        List<Domain> buildedDomains = new ArrayList<>();
        for (Domain domain : repository.findAll()) {
            buildedDomains.add(construct(domain));
        }
        return buildedDomains;
    }

    @Override
    public void store(Domain domain) {
        repository.save(domain);
    }

    public List<String> findDomainNamesNeedSync() {
        MatchOperation match = Aggregation.match(
                Criteria.where("needSync").exists(true));

        GroupOperation group = Aggregation.group().addToSet("name").as("strings");

        Aggregation aggregation = Aggregation.newAggregation(match, group);

        List<StringsContainer> stringsContainers =
                mongoOperations.aggregate(aggregation, Domain.class, StringsContainer.class).getMappedResults();

        if (stringsContainers == null || stringsContainers.isEmpty()) {
            return Collections.emptyList();
        } else {
            return stringsContainers.get(0).getStrings();
        }
    }

    public void syncRegSpec(String name) {
        logger.info("Sync " + name);
        RegSpec regSpec = null;
        try {
            regSpec = registrar.getRegSpec(name);
        } catch (Exception e) {
            logger.error("Can't getRegSpec, domainName: " + name + " " + e.getClass().toString() + " e.message: " + e.getMessage());
        }

        if (regSpec == null
                || regSpec.getFreeDate() == null
                || regSpec.getPaidTill() == null
                || regSpec.getRegistrar() == null
        ) {
            Map<String, String> params = new HashMap<>();
            params.put("name", name);

            Domain domain = build(params);

            if (domain.getNeedSync().isBefore(LocalDateTime.now().plusHours(1))) {
                logger.error("Can't syncRegSpec " + domain.getName() + " after register or renew more than one hour");
                mongoOperations.updateFirst(
                        new Query(new Criteria("_id").is(domain.getId())),
                        new Update().unset("needSync"), Domain.class
                );
            }
        } else {
            updateRegSpec(name, regSpec);
        }
    }

    private String getTaskExecutorRoutingKey(WebSite webSite) throws ParameterValidationException {
        try {
            String serverName = null;
            if (webSite != null) {
                serverName = staffRcClient.getServerByServiceId(webSite.getServiceId()).getName();
            }

            return TE + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[getTaskExecutorRoutingKey] got exception: %s ; resource id: %s class: %s"
                    , e.getMessage(), webSite != null ? webSite.getId() : null, webSite != null ? webSite.getClass().getSimpleName() : null);
            throw new ParameterValidationException("Exception: " + e.getMessage());
        }
    }
}
