package ru.majordomo.hms.rc.user.managers;

import com.google.i18n.phonenumbers.NumberParseException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.PhoneNumberManager;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.LegalEntity;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Service
public class GovernorOfPerson extends LordOfResources {
    private PersonRepository repository;
    private Cleaner cleaner;

    @Autowired
    public void setRepository(PersonRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Person person;
        try {
            person = (Person) buildResourceFromServiceMessage(serviceMessage);
            validate(person);
            store(person);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return person;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {

        String actionId = serviceMessage.getActionIdentity();
        String operationId = serviceMessage.getOperationIdentity();
        logger.debug("Action ID:" + actionId +
            "Operation ID:" + operationId +
            "Приступаю к построению ресурса, исходя из данных в ServiceMessage");

        Person person = new Person();
        LordOfResources.setResourceParams(person, serviceMessage, cleaner);
        String country = cleaner.cleanString((String) serviceMessage.getParam("country"));
        String postalAddress = cleaner.cleanString((String) serviceMessage.getParam("postalAddress"));
        Boolean owner = (Boolean) serviceMessage.getParam("owner");
        List<String> phoneNumbers = new ArrayList<>();
        if (serviceMessage.getParam("phoneNumbers") != null) {
            phoneNumbers = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("phoneNumbers"));
        }
        List<String> emailAddresses = new ArrayList<>();
        if (serviceMessage.getParam("emailAddresses") != null) {
            emailAddresses = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("emailAddresses"));
        }
        Object object = (Object) serviceMessage.getParam("passport");
        Passport passport = null;
        if (object != null) {
            passport = new Passport();
            Map<String, String> passportMap = (Map<String, String>) object;
            passport.setNumber(passportMap.get("number"));
            passport.setIssuedOrg(passportMap.get("issuedOrg"));
            passport.setIssuedDate(passportMap.get("issuedDate"));
            passport.setBirthday(passportMap.get("birthday"));
            passport.setMainPage(passportMap.get("mainPage"));
            passport.setRegisterPage(passportMap.get("registerPage"));
            passport.setAddress(passportMap.get("address"));
        }

        object = (Object) serviceMessage.getParam("legalEntity");
        LegalEntity legalEntity = null;
        if (object != null) {
            legalEntity = new LegalEntity();
            Map<String, String> legalEntityMap = (Map<String, String>) object;
            legalEntity.setInn(legalEntityMap.get("inn"));
            legalEntity.setOkpo(legalEntityMap.get("okpo"));
            legalEntity.setKpp(legalEntityMap.get("kpp"));
            legalEntity.setOgrn(legalEntityMap.get("ogrn"));
            legalEntity.setOkvedCodes(legalEntityMap.get("okved"));
            legalEntity.setAddress(legalEntityMap.get("address"));
        }
        String nicHandle = cleaner.cleanString((String) serviceMessage.getParam("nicHandle"));

        person.setPhoneNumbers(phoneNumbers);
        person.setEmailAddresses(emailAddresses);
        person.setPassport(passport);
        person.setLegalEntity(legalEntity);
        person.setCountry(country);
        person.setPostalAddress(postalAddress);
        person.setOwner(owner);
        person.setNicHandle(nicHandle);

        logger.debug("Action ID: " + actionId +
                " Operation Id: " + operationId +
                " ресурс Person построен из полученного сообщения:" + person.toString());

        return person;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Person person = (Person) resource;
        if (person.getAccountId() == null) {
            throw new ParameterValidateException("Аккаунт ID не может быть пустым");
        }

        if (person.getAccountId().equals("")) {
            throw new ParameterValidateException("Аккаунт ID не может быть пустым");
        }

        if (person.getName() == null) {
            throw new ParameterValidateException("Имя персоны не может быть пустым");
        }

        if (person.getName().equals("")) {
            throw new ParameterValidateException("Имя персоны не может быть пустым");
        }

        if (person.getSwitchedOn() == null) {
            person.setSwitchedOn(true);
        }

        if (person.getEmailAddresses() == null) {
            throw new ParameterValidateException("Должен быть указан хотя бы 1 email адрес");
        }

        if (person.getEmailAddresses().size() == 0) {
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

        if (person.getOwner() == null) {
            List<Person> persons = repository.findByAccountId(person.getAccountId());
            Boolean accHasOwner = false;
            for (Person storedPerson: persons) {
                if (storedPerson.getOwner()) {
                    accHasOwner = true;
                }
            }
            if (!accHasOwner) {
                person.setOwner(true);
            }
        }

        if (person.getCountry() == null || person.getCountry().equals("")) {
            person.setCountry("RU");
        }

        if (person.getPassport() != null && person.getLegalEntity() != null) {
            throw new ParameterValidateException("Passport и LegalEntity не могут быть указаны вместе");
        }
    }

    @Override
    protected Resource construct(Resource resource) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Person person = repository.findOne(resourceId);
        if (person == null) {
            throw new ParameterValidateException("Person с ID:" + resourceId + " не найдена");
        }

        return person;
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Person person = new Person();

        if (hasResourceIdAndAccountId(keyValue)) {
            person = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        return person;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Person> buildedPersons = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            buildedPersons = repository.findByAccountId(keyValue.get("accountId"));
        }

        return buildedPersons;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        Person person = (Person) resource;
        repository.save(person);
    }

}
