package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.*;

@Service
public class GovernorOfSSLCertificate extends LordOfResources<SSLCertificate> {

    private SSLCertificateRepository repository;
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
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Override
    public SSLCertificate update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String name = (String) serviceMessage.getParam("name");
        SSLCertificate sslCertificate = repository.findByName(name);
        if (sslCertificate == null) {
            throw new ResourceNotFoundException();
        }
        sslCertificate.setSwitchedOn(true);
        return sslCertificate;
    }

    public SSLCertificate update(Map<String, Object> params) throws ParameterValidateException, ResourceNotFoundException {
        String resourceId = (String) params.get("resourceId");
        if (resourceId == null || resourceId.equals("")) {
            throw new ParameterValidateException("Необходимо указать resourceId");
        }

        SSLCertificate certificate = repository.findOne(resourceId);

        if (certificate == null) {
            throw new ResourceNotFoundException("Не найдено SSL сертификата с ID: " + resourceId);
        }

        params.remove(resourceId);
        try {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                switch (entry.getKey()) {
                    case "state":
                    case "switchedOn":
                        Boolean switchedOn = (Boolean) entry.getValue();
                        if (!certificate.getSwitchedOn().equals(switchedOn)) {
                            certificate.setSwitchedOn(switchedOn);
                        }
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validate(certificate);
        store(certificate);

        return certificate;
    }

    @Override
    public SSLCertificate create(ServiceMessage serviceMessage) throws ParameterValidateException {
        SSLCertificate sslCertificate;
        try {

            sslCertificate = buildResourceFromServiceMessage(serviceMessage);
            validate(sslCertificate);
            store(sslCertificate);

            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("name", sslCertificate.getName());
            keyValue.put("accountId", serviceMessage.getAccountId());
            Domain domain = governorOfDomain.build(keyValue);
            domain.setSslCertificateId(sslCertificate.getId());
            governorOfDomain.validate(domain);
            governorOfDomain.store(domain);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return sslCertificate;
    }

    @Override
    public void preDelete(String resourceId) {

    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        SSLCertificate certificate = repository.findOne(resourceId);
        if (certificate == null) {
            throw new ResourceNotFoundException("Не найдено SSL сертификата с ID: " + resourceId);
        }
        certificate.setSwitchedOn(false);

        preDelete(resourceId);
        repository.save(certificate);
    }

    @Override
    protected SSLCertificate buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        SSLCertificate sslCertificate;
        ObjectMapper mapper = new ObjectMapper();
        try {
//            sslCertificate = (SSLCertificate) serviceMessage.getParam("sslCertificate");
//            LinkedHashMap<String, String> map = (LinkedHashMap<String, String>) serviceMessage.getParam("sslCertificate");
//            String json = mapper.writeValueAsString(map);
            String json = (String) serviceMessage.getParam("sslCertificate");
            sslCertificate = mapper.readValue(json, SSLCertificate.class);
        } catch (IOException e) {
            throw new ParameterValidateException(e.getMessage());
        }

        sslCertificate.setAccountId(serviceMessage.getAccountId());

        return sslCertificate;
    }

    @Override
    protected SSLCertificate construct(SSLCertificate sslCertificate) throws ParameterValidateException {
        throw new NotImplementedException();
    }

    @Override
    public void validate(SSLCertificate sslCertificate) throws ParameterValidateException {
        if (sslCertificate.getName() == null || sslCertificate.getName().equals("")) {
            throw new ParameterValidateException("Имя домена должно быть указано");
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("name", sslCertificate.getName());

        Domain domain = governorOfDomain.build(properties);
        if (domain == null) {
            throw new ParameterValidateException("Домен с указанным именем не найден");
        }
    }

    public boolean exists(Map<String, String> keyValue) {
        SSLCertificate certificate = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            certificate = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (hasNameAndAccountId(keyValue)) {
            certificate = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
        }

        return certificate != null;
    }

    @Override
    public SSLCertificate build(String resourceId) throws ResourceNotFoundException {
        SSLCertificate sslCertificate = repository.findOne(resourceId);
        if (sslCertificate == null) {
            throw new ResourceNotFoundException("SSLCertificate с ID: " + resourceId + " не найден");
        }
        return sslCertificate;
    }

    @Override
    public SSLCertificate build(Map<String, String> keyValue) throws ResourceNotFoundException {
        SSLCertificate certificate = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            certificate = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (hasNameAndAccountId(keyValue)) {
            certificate = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
        }

        if (certificate == null) {
            throw new ResourceNotFoundException("Не найден SSL сертификат по данным: " + keyValue.toString());
        }

        return certificate;
    }

    @Override
    public Collection<SSLCertificate> buildAll() {
        return repository.findAll();
    }

    @Override
    public List<SSLCertificate> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
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
    public void store(SSLCertificate sslCertificate) {
        repository.save(sslCertificate);
    }

    public String getTERoutingKey(String sslCertificateId) throws ParameterValidateException {
        SSLCertificate sslCertificate = build(sslCertificateId);
        if (sslCertificate == null) {
            return null;
        }
        Domain domain = domainRepository.findBySslCertificateId(sslCertificate.getId());
        if (domain == null) {
            return null;
        }
        WebSite webSite = webSiteRepository.findByDomainIdsContains(domain.getId());
        if (webSite == null) {
            return null;
        }
        try {
            String serverName = staffRcClient.getServerByServiceId(webSite.getServiceId()).getName();
            return "te" + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            throw new ParameterValidateException("Exception: " + e.getMessage());
        }
    }

}
