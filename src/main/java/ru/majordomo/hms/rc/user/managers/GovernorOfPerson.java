package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.cleaner.Cleaner;
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
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Person person = new Person();
        LordOfResources.setResourceParams(person, serviceMessage, cleaner);
        List<String> phoneNumbers = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("phoneNumbers"));
        List<String> emailAddresses = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("emailAddresses"));
        Object object = (Object) serviceMessage.getParam("passport");
        Passport passport = null;
        if (object != null) {
            passport = new Passport();
            Map<String, String> passportMap = (Map<String,String>) object;
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
            Map<String, String> legalEntityMap = (Map<String,String>) object;
            legalEntity.setInn(legalEntityMap.get("inn"));
            legalEntity.setOkpo(legalEntityMap.get("okpo"));
            legalEntity.setKpp(legalEntityMap.get("kpp"));
            legalEntity.setOgrn(legalEntityMap.get("ogrn"));
            legalEntity.setOkvedCodes(legalEntityMap.get("okved"));
            legalEntity.setAddress(legalEntityMap.get("address"));
        }

        person.setPhoneNumbers(phoneNumbers);
        person.setEmailAddresses(emailAddresses);
        person.setPassport(passport);
        person.setLegalEntity(legalEntity);

        return person;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Person person = (Person) resource;
        if (person.getPassport() != null && person.getLegalEntity() != null) {
            throw new ParameterValidateException("Passport и LegalEntity не могут быть указаны вместе");
        }
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
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        Person person = (Person) resource;
        repository.save(person);
    }

}
