package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.*;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.CertificateHelper;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.validation.group.SSLCertificateChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.SSLCertificateImportChecks;

import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class GovernorOfSSLCertificate extends LordOfResources<SSLCertificate> {

    private SSLCertificateRepository repository;
    private GovernorOfDomain governorOfDomain;
    private DomainRepository domainRepository;
    private WebSiteRepository webSiteRepository;
    private GovernorOfRedirect governorOfRedirect;
    private GovernorOfWebSite governorOfWebSite;
    private StaffResourceControllerClient staffRcClient;
    private Cleaner cleaner;
    private Validator validator;
    private String applicationName;

    public GovernorOfSSLCertificate(OperationOversightRepository<SSLCertificate> operationOversightRepository) {
        super(operationOversightRepository);
    }

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
    public void setGovernorOfRedirect(GovernorOfRedirect governorOfRedirect) {
        this.governorOfRedirect = governorOfRedirect;
    }

    @Autowired
    public void setGovernorOfWebSite(GovernorOfWebSite governorOfWebSite) {
        this.governorOfWebSite = governorOfWebSite;
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

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * см. коммент к create (Логика Oversight не используется)
     */
    @Override
    public OperationOversight<SSLCertificate> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException {
        OperationOversight<SSLCertificate> ovs;

        SSLCertificate sslCertificate = this.updateWrapper(serviceMessage);

        ovs = sendToOversight(sslCertificate, ResourceAction.UPDATE, false, generateAffected(sslCertificate), null);

        return ovs;
    }

    private SSLCertificate updateWrapper(ServiceMessage serviceMessage) {
        String resourceId = (String) serviceMessage.getParam("resourceId");
        String accountId = serviceMessage.getAccountId();

        SSLCertificate certificate = repository.findByIdAndAccountId(resourceId, accountId);

        if (certificate == null) {
            throw new ResourceNotFoundException(
                    "На аккаунте " + accountId + " не найден ssl-сертификат с ID: " + resourceId
            );
        }

        if (certificate.isLocked()) {
            throw new ParameterValidationException("Сертификат в процессе обновления");
        }

        Boolean switchedOn = (Boolean) serviceMessage.getParam("switchedOn");

        if (switchedOn != null) {
            certificate.setSwitchedOn(switchedOn);
        }

        if (canCreateCustomCertificate(serviceMessage)) {
            setCustomCertDataAndValidateIt(certificate, serviceMessage);
        }

        return certificate;
    }

    /**
     * При создании SSL сертификата логика Oversight не используется, объект сразу сохраняется в коллекцию и его ID прописывается в домене
     * Методы completeOversight... переопределены и не сохраняют(удаляют) повторно объект(ы)
     */
    @Override
    public OperationOversight<SSLCertificate> createByOversight(ServiceMessage serviceMessage) throws ParameterValidationException {
        OperationOversight<SSLCertificate> ovs;

        SSLCertificate cert = this.createWrapper(serviceMessage);

        ovs = sendToOversight(cert, ResourceAction.CREATE, false, generateAffected(cert), null);

        return ovs;
    }

    private SSLCertificate createWrapper(ServiceMessage serviceMessage) {
        SSLCertificate sslCertificate;
        try {

            sslCertificate = buildResourceFromServiceMessage(serviceMessage);

            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("name", sslCertificate.getName());
            keyValue.put("accountId", serviceMessage.getAccountId());

            Domain domain = governorOfDomain.build(keyValue);

            validate(sslCertificate);
            store(sslCertificate);

            governorOfDomain.setSslCertificateId(domain, sslCertificate.getId());
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно:" + e.getMessage());
        }

        return sslCertificate;
    }

    private List<Resource> generateAffected(SSLCertificate sslCertificate) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("sslCertificateId", sslCertificate.getId());

        Domain domain;
        try {
            domain = governorOfDomain.build(keyValue);
        } catch (ResourceNotFoundException e) {
            return new ArrayList<>();
        }

        keyValue = new HashMap<>();
        keyValue.put("domainId", domain.getId());
        Optional<WebSite> webSite = Optional.empty();
        try {
            webSite = Optional.ofNullable(governorOfWebSite.build(keyValue));
        } catch (ResourceNotFoundException ignored) {}
        Optional<Redirect> redirect = Optional.empty();
        try {
            redirect = Optional.ofNullable(governorOfRedirect.build(keyValue));
        } catch (ResourceNotFoundException ignored) {}

        // В случае c SSL сертификатом affected ресурсы webSite и redirect нужны только для TE,
        // дополнительной логики измениия после получения результата от TE - не происходит
        // (они как-бы являются required, но отправляем в affected)
        List<Resource> affected = new ArrayList<>();
        webSite.ifPresent(affected::add);
        redirect.ifPresent(affected::add);

        return affected;
    }

    @Override
    public void preDelete(String resourceId) {

    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Не найдено SSL сертификата с ID: " + resourceId);
        }

        governorOfDomain.removeSslCertificateId(resourceId);

        repository.deleteById(resourceId);
    }

    @Override
    public OperationOversight<SSLCertificate> dropByOversight(String resourceId) throws ResourceNotFoundException {
        SSLCertificate cert = build(resourceId);
        List<Resource> affected = generateAffected(cert); //До того как сертификат удалён из базы
        drop(cert.getId());
        return sendToOversight(cert, ResourceAction.DELETE, false, affected, null);
    }

    /**
     * Только удаление Oversight, ресурс не трогаем
     */
    @Override
    public SSLCertificate completeOversightAndStore(OperationOversight<SSLCertificate> ovs) {
        removeOversight(ovs);

        return ovs.getResource();
    }

    /**
     * Только удаление Oversight, ресурс не трогаем
     */
    @Override
    public void completeOversightAndDelete(OperationOversight<SSLCertificate> ovs) {
        removeOversight(ovs);
    }

    @Override
    public SSLCertificate buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        SSLCertificate certificate = new SSLCertificate();
        certificate.setAccountId(serviceMessage.getAccountId());
        certificate.setName((String) serviceMessage.getParam("name"));

        setCustomCertDataAndValidateIt(certificate, serviceMessage);

        return certificate;
    }

    @Override
    protected SSLCertificate construct(SSLCertificate sslCertificate) throws ParameterValidationException {
        throw new NotImplementedException();
    }

    @Override
    public void validate(SSLCertificate sslCertificate) throws ParameterValidationException {
        Set<ConstraintViolation<SSLCertificate>> constraintViolations = validator.validate(sslCertificate, SSLCertificateChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("sslCertificate: " + sslCertificate + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(SSLCertificate sslCertificate) {
        Set<ConstraintViolation<SSLCertificate>> constraintViolations = validator.validate(sslCertificate, SSLCertificateImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] sslCertificate: " + sslCertificate + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    public boolean exists(Map<String, String> keyValue) {
        if (hasResourceIdAndAccountId(keyValue)) {
            return repository.existsByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (hasNameAndAccountId(keyValue)) {
            return repository.existsByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
        }

        if (keyValue.get("name") != null) {
            return repository.existsByName(keyValue.get("name"));
        }

        return false;
    }

    @Override
    public SSLCertificate build(String resourceId) throws ResourceNotFoundException {
        return repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("SSLCertificate с ID: " + resourceId + " не найден"));
    }

    public Stream<SSLCertificate> findAllStream() {
        return repository.findAll().stream();
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

        if (keyValue.get("accountId") != null) {
            buildedCertificates = repository.findByAccountId(keyValue.get("accountId"));
        }

        return buildedCertificates;
    }

    @Override
    public void store(SSLCertificate sslCertificate) {
        repository.save(sslCertificate);
    }

    public String getTERoutingKey(SSLCertificate sslCertificate) throws ParameterValidationException {
        Domain domain = domainRepository.findBySslCertificateIdAndAccountId(sslCertificate.getId(), sslCertificate.getAccountId());
        if (domain == null) {
            return null;
        }
        WebSite webSite = webSiteRepository.findByDomainIdsContainsAndAccountId(domain.getId(), domain.getAccountId());
        if (webSite == null) {
            return null;
        }
        try {
            String serverName = staffRcClient.getServerByServiceId(webSite.getServiceId()).getName();
            return TE + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            throw new ParameterValidationException("Exception: " + e.getMessage());
        }
    }

    public void setCustomCertDataAndValidateIt(SSLCertificate sslCertificate, ServiceMessage serviceMessage) {
        sslCertificate.setKey((String) serviceMessage.getParam("key"));

        String cert = (String) serviceMessage.getParam("cert");
        String chain = (String) serviceMessage.getParam("chain");

        List<Certificate> certificates = CertificateHelper.buildSequenceCertificateChain(
                cert != null && !cert.isEmpty() ? cert + chain : chain
        );

        sslCertificate.setCert(
                CertificateHelper.toPEM(
                        certificates.get(0)
                )
        );

        if (certificates.size() > 1) {
            sslCertificate.setChain(
                    CertificateHelper.toPEM(
                            certificates.subList(1, certificates.size())
                    )
            );
        }

        try {
            sslCertificate.setNotAfter(CertificateHelper.getNotAfter(sslCertificate));
        } catch (Exception ignore) {}

        sslCertificate.setIssuerInfo(
                CertificateHelper.getIssuerInfo(certificates.get(0))
        );

        CertificateHelper.validate(sslCertificate);
    }

    public boolean canCreateCustomCertificate(ServiceMessage serviceMessage) {
        return Stream.of("key", "chain").allMatch(key ->
                serviceMessage.getParam(key) instanceof String
                && !serviceMessage.getParam(key).toString().isEmpty()
        );
    }
}
