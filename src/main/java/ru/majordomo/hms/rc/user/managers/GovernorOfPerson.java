package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Address;
import ru.majordomo.hms.rc.user.resources.LegalEntity;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.PersonType;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonImportChecks;
import ru.majordomo.hms.rc.user.resources.validation.groupSequenceProvider.PersonGroupSequenceProvider;

import static ru.majordomo.hms.rc.user.common.Utils.mapContains;

@Service
public class GovernorOfPerson extends LordOfResources<Person> {
    private PersonRepository repository;
    private Cleaner cleaner;
    private DomainRegistrarClient domainRegistrarClient;
    private GovernorOfDomain governorOfDomain;
    private Validator validator;
    private final ObjectMapper mapper = new ObjectMapper();

    public GovernorOfPerson(OperationOversightRepository<Person> operationOversightRepository) {
        super(operationOversightRepository);
    }

    @Autowired
    public void setDomainRegistrarClient(DomainRegistrarClient domainRegistrarClient) {
        this.domainRegistrarClient = domainRegistrarClient;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setRepository(PersonRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public OperationOversight<Person> createByOversight(ServiceMessage serviceMessage) throws ParameterValidationException {
        OperationOversight<Person> ovs;

        Person person;
        try {
            person = buildResourceFromServiceMessage(serviceMessage);
        } catch (ClassCastException e) {
            throw new ParameterValidationException("???????????? ?????? ???????????????? ??????????????");
        }
        preValidate(person);
        validate(person);

        ovs = sendToOversight(person, ResourceAction.CREATE);

        return ovs;
    }

    /**
     * ???????????????? ?????????????? ?? ?????????? ???????????? ???????????????? ?????????????? ?? ???????????????????????????? Oversight.
     * ???????????????????????? ?????? ?????????????????????? ????????????
     */
    Person createPersonRegistrant(Person person) {
        preValidate(person);
        validate(person);

        createPersonInDomainRegistrar(person);

        store(person);
        return person;
    }

    private void createPersonInDomainRegistrar(Person person) {
        try {
            ResponseEntity responseEntity = domainRegistrarClient.createPerson(person);
            String location = responseEntity.getHeaders().getLocation().getPath();
            String nicHandle = location.substring(location.lastIndexOf('/') + 1);
            person.setNicHandle(nicHandle);
        } catch (FeignException e) {
            String errorReason = e.getMessage();
            log.debug("???????????? ?????? ???????????????? ??????????????:" + errorReason);
            String errorContent = errorReason.replaceAll(".*content:", "");
//            String errorMessage;
//            try {
//                StringBuilder errorCollector = new StringBuilder();
//                JsonNode obj = mapper.readTree(errorContent);
//                Iterator<JsonNode> errors = obj.get("errors").elements();
//                while (errors.hasNext()) {
//                    JsonNode error = errors.next();
//                    errorCollector.append(error.get("code").textValue()).append("\n");
//                }
//                errorMessage = errorCollector.toString();
//            } catch (IOException ex) {
//                errorMessage = "???????????? ?????? ?????????????????????? ????????????. ?????????????????? ?????????????? ??????????.";
//            }
            log.debug("???????????? ?????? ???????????????? ??????????????: " + errorContent);
            throw new ParameterValidationException(errorContent);
        }
    }

    @Override
    public OperationOversight<Person> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException {
        Person person = this.updateWrapper(serviceMessage);

        return sendToOversight(person, ResourceAction.UPDATE);
    }

    private Person updateWrapper(ServiceMessage serviceMessage) {
        String resourceId;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidationException("???? ???????????? resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Person person = build(keyValue);
        if (person.getNicHandle() != null && !person.getNicHandle().equals("")) {
            throw new ParameterValidationException("???????????? ?????????????? ???????????????????????????????? ?? ??????????????????????????, ?????? ?????????????????? ?????????? ???????????? ????????????????, ????????????????????, ???????????? ???? domain@majordomo.ru");
        }

        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "name":
                        person.setName(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "firstname":
                        person.setFirstname(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "lastname":
                        person.setLastname(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "middlename":
                        person.setMiddlename(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "orgName":
                        person.setOrgName(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "orgForm":
                        person.setOrgForm(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "type":
                        person.setType(PersonType.valueOf(cleaner.cleanString((String) entry.getValue())));
                        break;
                    case "country":
                        person.setCountry(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "phoneNumbers":
                        person.setPhoneNumbers(cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("phoneNumbers")));
                        break;
                    case "emailAddresses":
                        person.setEmailAddresses(cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("emailAddresses")));
                        break;
                    case "postalAddress":
                        Object postalAddressData = entry.getValue();
                        if (postalAddressData != null) {
                            Address postalAddress = mapper.convertValue(postalAddressData, Address.class);
                            person.setPostalAddress(postalAddress);
                        }
                        break;
                    case "nicHandle":
                        person.setNicHandle(cleaner.cleanString((String) serviceMessage.getParam("nicHandle")));
                        break;
                    case "passport":
                        Map<String, String> passportMap = (Map<String, String>) entry.getValue();
                        Passport passport = null;
                        if (passportMap != null && !isMapWithEmptyStrings(passportMap)) {
                            passport = buildPassportFromMap(passportMap);
                        }
                        person.setPassport(passport);
                        break;
                    case "legalEntity":
                        Map<String, Object> legalEntityMap = (Map<String, Object>) entry.getValue();
                        LegalEntity legalEntity = null;
                        if (legalEntityMap != null && !isObjectsMapWithEmptyStrings(legalEntityMap)) {
                            legalEntity = buildLegalEntityFromMap(legalEntityMap);
                        }
                        person.setLegalEntity(legalEntity);
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("???????? ???? ???????????????????? ???????????? ??????????????");
        }

        preValidate(person);
        validate(person);

        return person;
    }

    private boolean isMapWithEmptyStrings(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.equals("")) {
                return false;
            }
        }
        return true;
    }
    private boolean isObjectsMapWithEmptyStrings(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && !value.equals("")) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void preDelete(String resourceId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("personId", resourceId);

        if (governorOfDomain.buildAll(keyValue).size() > 0) {
            throw new ParameterValidationException("?????????????? ????????????, ???????????????????????????????????? ???? ???????????? ??????????????. ???????????????? ?????????????? ????????????????????");
        }
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("???? ?????????????? ?????????????? ?? ID: " + resourceId);
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    /**
     * ????????????????/?????????????????? ?????????????? ?? ???????????????????? ???????????????? Oversight
     */
    @Override
    public Person completeOversightAndStore(OperationOversight<Person> ovs) {
        if (ovs.getReplace()) {
            removeOldResource(ovs.getResource());
        }
        if (ovs.getResource().getNicHandle() == null || ovs.getResource().getNicHandle().equals("")) {
            createPersonInDomainRegistrar(ovs.getResource());
        }
        Person person = ovs.getResource();
        store(person);
        removeOversight(ovs);

        return person;
    }

    @Override
    public OperationOversight<Person> dropByOversight(String resourceId) throws ResourceNotFoundException {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("personId", resourceId);

        if (governorOfDomain.buildAll(keyValue).size() > 0) {
            throw new ParameterValidationException("?????????????? ????????????, ???????????????????????????????????? ???? ???????????? ??????????????. ???????????????? ?????????????? ????????????????????");
        }

        Person record = build(resourceId);
        return sendToOversight(record, ResourceAction.DELETE);
    }

    @Override
    public Person buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {

        String actionId = serviceMessage.getActionIdentity();
        String operationId = serviceMessage.getOperationIdentity();
        log.debug("Action ID:" + actionId +
                "Operation ID:" + operationId +
                "?????????????????? ?? ???????????????????? ??????????????, ???????????? ???? ???????????? ?? ServiceMessage");

        Person person = new Person();
        setResourceParams(person, serviceMessage, cleaner);
        PersonType type = PersonType.valueOf(cleaner.cleanString((String) serviceMessage.getParam("type")));
        String country = cleaner.cleanString((String) serviceMessage.getParam("country"));
        Map<String,String> postalAddressMap = (Map<String,String>) serviceMessage.getParam("postalAddress");
        Address postalAddress = null;
        if (postalAddressMap != null) {
            postalAddress = buildAddressFromMap(postalAddressMap);
        }
        List<String> phoneNumbers = new ArrayList<>();
        if (serviceMessage.getParam("phoneNumbers") != null) {
            phoneNumbers = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("phoneNumbers"));
        }
        List<String> emailAddresses = new ArrayList<>();
        if (serviceMessage.getParam("emailAddresses") != null) {
            emailAddresses = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("emailAddresses"));
        }
        Map<String, String> passportMap = (Map<String, String>) serviceMessage.getParam("passport");
        Passport passport = null;
        if (passportMap != null) {
            passport = buildPassportFromMap(passportMap);
        }

        Map<String, Object> legalEntityMap = (Map<String, Object>) serviceMessage.getParam("legalEntity");
        LegalEntity legalEntity = null;
        if (legalEntityMap != null) {
            legalEntity = buildLegalEntityFromMap(legalEntityMap);
        }
        String nicHandle = cleaner.cleanString((String) serviceMessage.getParam("nicHandle"));

        String firstname = cleaner.cleanString((String) serviceMessage.getParam("firstname"));
        String lastname = cleaner.cleanString((String) serviceMessage.getParam("lastname"));
        String middlename = cleaner.cleanString((String) serviceMessage.getParam("middlename"));
        String orgName = cleaner.cleanString((String) serviceMessage.getParam("orgName"));
        String orgForm = cleaner.cleanString((String) serviceMessage.getParam("orgForm"));

        person.setPhoneNumbers(phoneNumbers);
        person.setEmailAddresses(emailAddresses);
        person.setPassport(passport);
        person.setLegalEntity(legalEntity);
        person.setCountry(country);
        person.setType(type);
        person.setPostalAddress(postalAddress);
        person.setNicHandle(nicHandle);
        person.setFirstname(firstname);
        person.setLastname(lastname);
        person.setMiddlename(middlename);
        person.setOrgName(orgName);
        person.setOrgForm(orgForm);

        log.debug("Action ID: " + actionId +
                " Operation Id: " + operationId +
                " ???????????? Person ???????????????? ???? ?????????????????????? ??????????????????:" + person.toString());

        return person;
    }

    @Override
    public void preValidate(Person person) {
        if (person.getSwitchedOn() == null) {
            person.setSwitchedOn(true);
        }

        if (person.getCountry() == null || person.getCountry().equals("")) {
            person.setCountry("RU");
        }
    }

    @Override
    public void validate(Person person) throws ParameterValidationException {
        PersonGroupSequenceProvider personGroupSequenceProvider = new PersonGroupSequenceProvider();

        Set<ConstraintViolation<Person>> constraintViolations = validator.validate(
                person,
                personGroupSequenceProvider.getValidationGroupsCustom(person).toArray(new Class<?>[]{})
        );

        if (!constraintViolations.isEmpty()) {
            log.error("person: " + person + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Person person) {
        Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person, PersonImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] person: " + person + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected Person construct(Person person) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Person build(String resourceId) throws ResourceNotFoundException {
        return repository
                .findById(resourceId)
                .orElseThrow(() -> new ParameterValidationException("Person ?? ID:" + resourceId + " ???? ??????????????"));
    }

    @Override
    public Person build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Person person = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            person = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
            if (person == null) {
                person = repository.findByIdAndLinkedAccountIds(keyValue.get("resourceId"), keyValue.get("accountId"));
            }
        }

        if (person == null) {
            throw new ResourceNotFoundException("???? ?????????????? ?????????????? ?? ID: " + keyValue.get("resourceId")
                    + " ?????? ???????????????? ?? AccountID: " + keyValue.get("accountId"));
        }

        return person;
    }

    @Override
    public Collection<Person> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Person> buildedPersons = new ArrayList<>();

        if (mapContains(keyValue, "accountId", "nicHandle")) {
            buildedPersons = repository.findByAccountIdAndNicHandle(keyValue.get("accountId"), keyValue.get("nicHandle"));
        } else if (mapContains(keyValue, "accountId")) {
            buildedPersons = repository.findByAccountId(keyValue.get("accountId"));
            buildedPersons.addAll(repository.findByLinkedAccountIds(keyValue.get("accountId")));
        }

        return buildedPersons;
    }

    @Override
    public Collection<Person> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Person person) {
        repository.save(person);
    }

    public Passport buildPassportFromMap(Map<String, String> passportMap) {
        Passport passport = new Passport();
        if (passportMap.get("number") != null && !passportMap.get("number").equals("")) {
            passport.setNumber(passportMap.get("number"));
        }
        if (passportMap.get("document") != null && !passportMap.get("document").equals("")) {
            passport.setDocument(passportMap.get("document"));
        }
        passport.setIssuedOrg(passportMap.get("issuedOrg"));
        if (passportMap.get("issuedDate") != null && !passportMap.get("issuedDate").equals("")) {
            passport.setIssuedDate(passportMap.get("issuedDate"));
        }
        if (passportMap.get("birthday") != null && !passportMap.get("birthday").equals("")) {
            passport.setBirthday(passportMap.get("birthday"));
        }
        passport.setMainPage(passportMap.get("mainPage"));
        passport.setRegisterPage(passportMap.get("registerPage"));
        return passport;
    }

    public LegalEntity buildLegalEntityFromMap(Map<String, Object> legalEntityMap) {
        LegalEntity legalEntity = new LegalEntity();
        legalEntity.setInn((String) legalEntityMap.get("inn"));
        legalEntity.setKpp((String) legalEntityMap.get("kpp"));
        legalEntity.setOgrn((String) legalEntityMap.get("ogrn"));
        legalEntity.setBankName((String) legalEntityMap.get("bankName"));
        legalEntity.setBik((String) legalEntityMap.get("bik"));
        legalEntity.setCorrespondentAccount((String) legalEntityMap.get("correspondentAccount"));
        legalEntity.setBankAccount((String) legalEntityMap.get("bankAccount"));
        legalEntity.setDirectorFirstname((String) legalEntityMap.get("directorFirstname"));
        legalEntity.setDirectorLastname((String) legalEntityMap.get("directorLastname"));
        legalEntity.setDirectorMiddlename((String) legalEntityMap.get("directorMiddlename"));

        Map<String,String> postalAddressMap = (HashMap<String,String>) legalEntityMap.get("address");
        Address postalAddress = null;
        if (postalAddressMap != null) {
            postalAddress = buildAddressFromMap(postalAddressMap);
        }

        legalEntity.setAddress(postalAddress);

        return legalEntity;
    }

    public Address buildAddressFromMap(Map<String,String> addressMap) {
        Address address = new Address();
        String zip = addressMap.get("zip");
        address.setZip(zip);
        address.setCity(addressMap.get("city"));
        address.setStreet(addressMap.get("street"));

        return address;
    }

    public Stream<Person> findPersonsWithNicHandlesByNicHandleNotBlank() {
        return repository.findPersonsWithNicHandlesByNicHandleNotBlank();
    }

    private void prepareAndStore(Person localPerson, Person domainRegPerson) {

        domainRegPerson.setId(localPerson.getId());
        domainRegPerson.setAccountId(localPerson.getAccountId());
        domainRegPerson.setSwitchedOn(true);
        preValidate(domainRegPerson);

        store(domainRegPerson);
    }

    public void manualSync(Person person) {
        Person personFromDomainRegistrar = domainRegistrarClient.getPerson(person.getNicHandle());

        if (personFromDomainRegistrar != null) {
            prepareAndStore(person, personFromDomainRegistrar);
        } else {
            throw new ResourceNotFoundException("?????????????? ?? nicHandle: " + person.getNicHandle() + " ???? ?????????????? " +
                    "?? ????????????????????????, ???????? ???? ?????????????????? ???? ?????????????????????? ???????????????? c Majordomo.");
        }
    }

    public void sync(Person person) {

        Person personFromDomainRegistrar = domainRegistrarClient.getPerson(person.getNicHandle());

        if (personFromDomainRegistrar != null) {
            try {
                prepareAndStore(person, personFromDomainRegistrar);
                return;

            } catch (ParameterValidationException | ConstraintViolationException e) {
                e.printStackTrace();
            }
        }

        person.setSwitchedOn(false);
        store(person);
    }

    public Person addByNicHandle(String accountId, String nicHandle) {
        Person personFromDomainRegistrar = domainRegistrarClient.getPerson(nicHandle);

        if (personFromDomainRegistrar != null) {
            personFromDomainRegistrar.setAccountId(accountId);
            personFromDomainRegistrar.setSwitchedOn(true);

            try {
                preValidate(personFromDomainRegistrar);
                //?????? ???????????????????? ???????????????????????? ?? ?????????????????????? ??????????????, ??????????????, ?????? ?????? ??????????????
                //validate(personFromDomainRegistrar);
                store(personFromDomainRegistrar);
            } catch (ParameterValidationException | ConstraintViolationException e) {
                e.printStackTrace();
                throw e;
            }

            return personFromDomainRegistrar;
        } else {
            throw new ResourceNotFoundException("?????????????? ?? nicHandle: " + nicHandle + " ???? ?????????????? " +
                    "?? ????????????????????????, ???????? ???? ?????????????????? ???? ?????????????????????? ???????????????? c Majordomo.");
        }
    }

    public Person setLocked(String accountId, String personId, Boolean lockState) {
        if (lockState == null) {
            throw new ParameterValidationException("???? ???????????????? ?????????????????? Lock'??");
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        keyValue.put("resourceId", personId);
        Person person = build(keyValue);

        person.setLocked(lockState);
        store(person);

        return person;
    }
}
