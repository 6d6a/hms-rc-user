package ru.majordomo.hms.rc.user.managers;

import com.google.i18n.phonenumbers.NumberParseException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.PhoneNumberManager;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.LegalEntity;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Service
public class GovernorOfPerson extends LordOfResources<Person> {
    private PersonRepository repository;
    private Cleaner cleaner;
    private DomainRegistrarClient domainRegistrarClient;
    private GovernorOfDomain governorOfDomain;

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

    @Override
    public Person create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Person person;
        try {
            person = buildResourceFromServiceMessage(serviceMessage);
            validate(person);

            try {
                ResponseEntity responseEntity = domainRegistrarClient.createPerson(person);
                String location = responseEntity.getHeaders().getLocation().getPath();
                String nicHandle = location.substring(location.lastIndexOf('/') + 1);
                person.setNicHandle(nicHandle);
            } catch (Exception e) {
                logger.error("Не удалось создать персону с ID " + person.getId() + " в DOMAIN-REGISTRAR");
            }

            store(person);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return person;
    }

    @Override
    public Person update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String resourceId;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidateException("Не указан resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Person person = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "name":
                        person.setName(cleaner.cleanString((String) entry.getValue()));
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
                        person.setPostalAddress(cleaner.cleanString((String) serviceMessage.getParam("postalAddress")));
                        break;
                    case "passport":
                        Map<String, String> passportMap = (Map<String, String>) entry.getValue();
                        Passport passport = null;
                        if (passportMap != null) {
                            passport = buildPassportFromMap(passportMap);
                        }
                        person.setPassport(passport);
                        break;
                    case "legalEntity":
                        Map<String, String> legalEntityMap = (Map<String, String>) entry.getValue();
                        LegalEntity legalEntity = null;
                        if (legalEntityMap != null) {
                            legalEntity = buildLegalEntityFromMap(legalEntityMap);
                        }
                        person.setLegalEntity(legalEntity);
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validate(person);

        try {
            domainRegistrarClient.updatePerson(person.getNicHandle(), person);
        } catch (Exception e) {
            logger.error("Не удалось обновить персону с ID " + person.getId() + " в DOMAIN-REGISTRAR");
        }

        store(person);

        return person;
    }

    @Override
    public void preDelete(String resourceId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("personId", resourceId);

        if (governorOfDomain.buildAll(keyValue).size() > 0) {
            throw new ParameterValidateException("Имеется");
        }
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Не найдена персона с ID: " + resourceId);
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected Person buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {

        String actionId = serviceMessage.getActionIdentity();
        String operationId = serviceMessage.getOperationIdentity();
        logger.debug("Action ID:" + actionId +
            "Operation ID:" + operationId +
            "Приступаю к построению ресурса, исходя из данных в ServiceMessage");

        Person person = new Person();
        LordOfResources.setResourceParams(person, serviceMessage, cleaner);
        String country = cleaner.cleanString((String) serviceMessage.getParam("country"));
        String postalAddress = cleaner.cleanString((String) serviceMessage.getParam("postalAddress"));
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

        Map<String, String> legalEntityMap = (Map<String, String>) serviceMessage.getParam("legalEntity");
        LegalEntity legalEntity = null;
        if (legalEntityMap != null) {
            legalEntity = buildLegalEntityFromMap(legalEntityMap);
        }
        String nicHandle = cleaner.cleanString((String) serviceMessage.getParam("nicHandle"));

        person.setPhoneNumbers(phoneNumbers);
        person.setEmailAddresses(emailAddresses);
        person.setPassport(passport);
        person.setLegalEntity(legalEntity);
        person.setCountry(country);
        person.setPostalAddress(postalAddress);
        person.setNicHandle(nicHandle);

        logger.debug("Action ID: " + actionId +
                " Operation Id: " + operationId +
                " ресурс Person построен из полученного сообщения:" + person.toString());

        return person;
    }

    @Override
    public void validate(Person person) throws ParameterValidateException {
        if (person.getAccountId() == null || person.getAccountId().equals("")) {
            throw new ParameterValidateException("Аккаунт ID не может быть пустым");
        }

        if (person.getName() == null || person.getName().equals("")) {
            throw new ParameterValidateException("Имя персоны не может быть пустым");
        }

        if (person.getSwitchedOn() == null) {
            person.setSwitchedOn(true);
        }

        if (person.getEmailAddresses() == null || person.getEmailAddresses().size() == 0) {
            throw new ParameterValidateException("Должен быть указан хотя бы 1 email адрес");
        }

        EmailValidator validator = EmailValidator.getInstance(true, true); //allowLocal, allowTLD
        for (String emailAddress: person.getEmailAddresses()) {
            if (!validator.isValid(emailAddress)) {
                throw new ParameterValidateException("Адрес " + emailAddress + " некорректен");
            }
        }

        if (person.getPhoneNumbers() != null) {
            for (String phoneNumber: person.getPhoneNumbers()) {
                try {
                    if(!PhoneNumberManager.phoneValid(phoneNumber)) {
                        throw new ParameterValidateException("Номер: " + phoneNumber + " некорректен");
                    }
                } catch (NumberParseException e) {
                    throw new ParameterValidateException("Номер: " + phoneNumber + " некорректен");
                }
            }
        }

        if (person.getCountry() == null || person.getCountry().equals("")) {
            person.setCountry("RU");
        }
    }

    @Override
    protected Person construct(Person person) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Person build(String resourceId) throws ResourceNotFoundException {
        Person person = repository.findOne(resourceId);
        if (person == null) {
            throw new ParameterValidateException("Person с ID:" + resourceId + " не найдена");
        }

        return person;
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
            throw new ResourceNotFoundException("Не найдена персона с ID: " + keyValue.get("resourceId")
                    + " для аккаунта с AccountID: " + keyValue.get("accountId"));
        }

        return person;
    }

    @Override
    public Collection<Person> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Person> buildedPersons = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
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
        passport.setNumber(passportMap.get("number"));
        passport.setIssuedOrg(passportMap.get("issuedOrg"));
        passport.setIssuedDate(passportMap.get("issuedDate"));
        passport.setBirthday(passportMap.get("birthday"));
        passport.setMainPage(passportMap.get("mainPage"));
        passport.setRegisterPage(passportMap.get("registerPage"));
        passport.setAddress(passportMap.get("address"));
        return passport;
    }

    public LegalEntity buildLegalEntityFromMap(Map<String, String> legalEntityMap) {
        LegalEntity legalEntity = new LegalEntity();
        legalEntity.setInn(legalEntityMap.get("inn"));
        legalEntity.setOkpo(legalEntityMap.get("okpo"));
        legalEntity.setKpp(legalEntityMap.get("kpp"));
        legalEntity.setOgrn(legalEntityMap.get("ogrn"));
        legalEntity.setOkvedCodes(legalEntityMap.get("okved"));
        legalEntity.setAddress(legalEntityMap.get("address"));
        legalEntity.setBankName(legalEntityMap.get("bankName"));
        legalEntity.setBik(legalEntityMap.get("bik"));
        legalEntity.setCorrespondentAccount(legalEntityMap.get("correspondentAccount"));
        legalEntity.setBankAccount(legalEntityMap.get("bankAccount"));
        return legalEntity;
    }

}
