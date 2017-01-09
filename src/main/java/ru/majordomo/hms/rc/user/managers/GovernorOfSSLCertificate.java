package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.repositories.SslCertificateActionIdentityRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.SslCertificateActionIdentity;

@Service
public class GovernorOfSSLCertificate extends LordOfResources {

    private SSLCertificateRepository repository;
    private SslCertificateActionIdentityRepository actionIdentityRepository;
    private GovernorOfDomain governorOfDomain;
    private DomainRepository domainRepository;
    private WebSiteRepository webSiteRepository;
    private StaffResourceControllerClient staffRcClient;
    private Cleaner cleaner;

    private String applicationName;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Autowired
    public void setDomainRepository(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Autowired
    public void setWebSiteRepository(WebSiteRepository webSiteRepository) {
        this.webSiteRepository = webSiteRepository;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setRepository(SSLCertificateRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setActionIdentityRepository(SslCertificateActionIdentityRepository actionIdentityRepository) {
        this.actionIdentityRepository = actionIdentityRepository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    public Resource update(Map<String, Object> params) throws ParameterValidateException, ResourceNotFoundException {
        String resourceId = (String) params.get("resourceId");
        if (resourceId == null || resourceId.equals("")) {
            throw new ParameterValidateException("Необходимо указать resourceId");
        }

        SSLCertificate certificate = repository.findOne(resourceId);

        if (certificate == null) {
            throw new ResourceNotFoundException("Не найдено SSL сертификата с ID: " + resourceId);
        }

        Boolean actionNeeded = false;

        params.remove(resourceId);
        try {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                switch (entry.getKey()) {
                    case "state":
                        if (entry.getValue() instanceof SSLCertificateState) {
                            certificate.setState((SSLCertificateState) entry.getValue());
                            if (entry.getValue().equals("ISSUED")) {
                                actionNeeded = true;
                            }
                        }
                        break;
                    case "switchedOn":
                        Boolean switchedOn = (Boolean) entry.getValue();
                        if (!certificate.getSwitchedOn().equals(switchedOn)) {
                            certificate.setSwitchedOn(switchedOn);
                            actionNeeded = true;
                        }
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        SslCertificateActionIdentity actionIdentity = actionIdentityRepository.findBySslCertificateId(certificate.getId());
        if (actionNeeded) {

        }

        validate(certificate);
        store(certificate);

        return certificate;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        SSLCertificate sslCertificate;
        try {

            sslCertificate = (SSLCertificate) buildResourceFromServiceMessage(serviceMessage);
            validate(sslCertificate);
            store(sslCertificate);

            if (sslCertificate.getState().equals(SSLCertificateState.NEW)) {
                SslCertificateActionIdentity actionIdentity = new SslCertificateActionIdentity();
                actionIdentity.setActionIdentity(serviceMessage.getActionIdentity());
                actionIdentity.setSslCertificateId(sslCertificate.getId());
                actionIdentityRepository.save(actionIdentity);

                Map<String, String> keyValue = new HashMap<>();
                keyValue.put("name", sslCertificate.getName());
                keyValue.put("accountId", sslCertificate.getAccountId());
                Domain domain = (Domain) governorOfDomain.build(keyValue);
                domain.setSslCertificateId(sslCertificate.getId());
                governorOfDomain.validate(domain);
                governorOfDomain.store(domain);
            }

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return sslCertificate;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        SSLCertificate certificate = repository.findOne(resourceId);
        if (certificate == null) {
            throw new ResourceNotFoundException("Не найдено SSL сертификата с ID: " + resourceId);
        }
        certificate.setSwitchedOn(false);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", certificate.getName());
        keyValue.put("accountId", certificate.getAccountId());
        Domain domain = (Domain) governorOfDomain.build(keyValue);
        domain.setSslCertificateId(null);
        governorOfDomain.validate(domain);
        governorOfDomain.store(domain);

        repository.save(certificate);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        String name = (String) serviceMessage.getParam("name");
        SSLCertificate sslCertificate = repository.findByName(name);
        if (sslCertificate != null) {
            sslCertificate.setSwitchedOn(true);
            return sslCertificate;
        }

        sslCertificate = new SSLCertificate();
        LordOfResources.setResourceParams(sslCertificate, serviceMessage, cleaner);

        sslCertificate.setName(name);
        sslCertificate.setState(SSLCertificateState.NEW);

        return sslCertificate;
    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        throw new NotImplementedException();
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        SSLCertificate sslCertificate = (SSLCertificate) resource;
        if (sslCertificate.getName() == null || sslCertificate.getName().equals("")) {
            throw new ParameterValidateException("Имя домена должно быть указано");
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("name", sslCertificate.getName());

        List<Domain> foundDomains = (List<Domain>) governorOfDomain.build(properties);
        if (foundDomains == null || foundDomains.isEmpty()) {
            throw new ParameterValidateException("Домен с указанным именем не найден");
        }
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        SSLCertificate sslCertificate = repository.findOne(resourceId);
        if (sslCertificate == null) {
            throw new ResourceNotFoundException("SSLCertificate с ID: " + resourceId + " не найден");
        }
        return sslCertificate;
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        SSLCertificate certificate = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            certificate = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (certificate == null) {
            throw new ResourceNotFoundException("Не найден SSL сертификат");
        }

        return certificate;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public List<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<SSLCertificate> buildedCertificates = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            buildedCertificates = repository.findByAccountId(keyValue.get("accountId"));
        }

        return buildedCertificates;
    }

    @Override
    public void store(Resource resource) {
        repository.save((SSLCertificate) resource);
    }

    public String getTaskExecutorRoutingKeyForSslCertificate(String sslCertificateId) throws ParameterValidateException {
        SSLCertificate sslCertificate = (SSLCertificate) build(sslCertificateId);
        if (sslCertificate == null) {
            throw new ParameterValidateException("Не найден SSL сертифика");
        }
        Domain domain = domainRepository.findBySslCertificateId(sslCertificate.getId());
        if (domain == null) {
            throw new ParameterValidateException("Не найден домен для SSL сертификата с ID: " + sslCertificate.getId());
        }
        WebSite webSite = webSiteRepository.findByDomainIds(domain.getId());
        if (webSite == null) {
            throw new ParameterValidateException("Не найден webSite для домена с ID: " + domain.getId());
        }
        try {
            String serverName = staffRcClient.getServerByServiceId(webSite.getServiceId()).getName();
            return "te" + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            throw new ParameterValidateException("Exception: " + e.getMessage());
        }
    }

    public ServiceMessage createSslCertificateServiceMessageForTE(String sslCertificateId) {
        SSLCertificate certificate = (SSLCertificate) build(sslCertificateId);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(certificate.getAccountId());
        SslCertificateActionIdentity actionIdentity = actionIdentityRepository.findOne(sslCertificateId);
        if (actionIdentity != null) {
            serviceMessage.setActionIdentity(actionIdentity.getActionIdentity());
        }
        serviceMessage.setObjRef("http://" + applicationName + "/ssl-certificate/" + sslCertificateId);
        serviceMessage.addParam("success", true);
        return serviceMessage;
    }

}
