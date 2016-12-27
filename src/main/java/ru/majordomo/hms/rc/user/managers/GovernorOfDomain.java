package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Service
public class GovernorOfDomain extends LordOfResources {

    private Cleaner cleaner;
    private DomainRepository repository;
    private GovernorOfPerson governorOfPerson;
    private GovernorOfSSLCertificate governorOfSSLCertificate;
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
                needRegister =(Boolean) serviceMessage.getParam("register");
            }

            domain = (Domain) buildResourceFromServiceMessage(serviceMessage);
            validate(domain);
            if (needRegister != null && needRegister) {
                try {
                     registrar.registerDomain(domain.getPerson().getNicHandle(), domain.getName());
                } catch (Exception e) {
                    throw new ParameterValidateException();
                }
                try {
                    domain.setRegSpec(registrar.getRegSpec(domain.getName()));
                } catch (Exception e) {
                    throw new ParameterValidateException();
                }
            }
            store(domain);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return domain;
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
        Domain domain = new Domain();
        LordOfResources.setResourceParams(domain, serviceMessage, cleaner);
        String domainPersonId = null;
        if (serviceMessage.getParam("personId") != null) {
             domainPersonId = cleaner.cleanString((String) serviceMessage.getParam("personId"));
        }
        domain.setPerson((Person) governorOfPerson.build(domainPersonId));
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
        if (domainPerson == null) {
            throw new ParameterValidateException("Персона должна быть указана");
        }
        governorOfPerson.validate(domainPerson);

        SSLCertificate sslCertificate = domain.getSslCertificate();
        if (sslCertificate != null) {
            governorOfSSLCertificate.validate(sslCertificate);
        }
    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        Domain domain = (Domain) resource;
        Person domainPerson = (Person) governorOfPerson.build(domain.getPersonId());
        domain.setPerson(domainPerson);
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
        }

        if (hasNameAndAccountId(keyValue)) {
            domain = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
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

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            for (Domain domain : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedDomains.add((Domain) construct(domain));
            }
        }

        return buildedDomains;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<Domain> buildedDomains = new ArrayList<>();
        for (Domain domain: repository.findAll()) {
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

    }

}
