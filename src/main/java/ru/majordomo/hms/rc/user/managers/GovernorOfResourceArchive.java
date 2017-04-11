package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.ResourceArchiveRepository;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;
import ru.majordomo.hms.rc.user.resources.Serviceable;
import ru.majordomo.hms.rc.user.validation.group.ResourceArchiveChecks;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

@Service
public class GovernorOfResourceArchive extends LordOfResources<ResourceArchive> {
    private ResourceArchiveRepository repository;
    private GovernorOfWebSite governorOfWebSite;
    private GovernorOfDatabase governorOfDatabase;
    private String archiveHostname;
    private Validator validator;
    private Cleaner cleaner;

    @Autowired
    public void setRepository(ResourceArchiveRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setGovernorOfWebSite(GovernorOfWebSite governorOfWebSite) {
        this.governorOfWebSite = governorOfWebSite;
    }

    @Autowired
    public void setGovernorOfDatabase(GovernorOfDatabase governorOfDatabase) {
        this.governorOfDatabase = governorOfDatabase;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Value("${default.archive.hostname}")
    public void setArchiveHostname(String archiveHostname) {
        this.archiveHostname = archiveHostname;
    }

    @Override
    public ResourceArchive update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException {
        throw new NotImplementedException();
    }

    public void dropByResourceId(String resourceId) {
        ResourceArchive archive = repository.findByResourceId(resourceId);
        if (archive != null) {
            repository.delete(resourceId);
        }
    }

    @Override
    public void preDelete(String resourceId) {

    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (resourceId == null) {
            throw new ResourceNotFoundException();
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected ResourceArchive buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        ResourceArchive archive = new ResourceArchive();
        setResourceParams(archive, serviceMessage, cleaner);
        String resourceId;
        ResourceArchiveType type = null;

        try {
            resourceId = cleaner.cleanString((String) serviceMessage.getParam("resourceId"));
            if (serviceMessage.getParam("resourceType") != null) {
                for (ResourceArchiveType resourceArchiveType : ResourceArchiveType.values()) {
                    if (resourceArchiveType.name().equals(serviceMessage.getParam("resourceType").toString())) {
                        type = ResourceArchiveType.valueOf(serviceMessage.getParam("resourceType").toString());
                        break;
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        if (resourceId == null) {
            throw new ParameterValidateException("Не указан resourceId");
        } else {
            if (repository.findByResourceId(resourceId) != null) {
                throw new ParameterValidateException("Одновременно может хранитсья только один массив");
            }
            archive.setResourceId(resourceId);
        }
        if (type == null) {
            throw new ParameterValidateException("Не указан resourceType");
        } else {
            archive.setResourceType(type);
        }

        SecureRandom random = new SecureRandom();
        String filename = new BigInteger(260, random).toString(32);
        String fileLink = "http://" + archiveHostname + "/" + filename;
        archive.setFileLink(fileLink);

        try {
            LordOfResources governor = getGovernor(archive);
            archive.setServiceId(((Serviceable) governor.build(resourceId)).getServiceId());
        } catch (Exception e) {
            throw new ParameterValidateException("Ошибка при поиске сервера");
        }

        return archive;
    }

    @Override
    public void validate(ResourceArchive archive) throws ParameterValidateException {
        Set<ConstraintViolation<ResourceArchive>> constraintViolations = validator.validate(archive, ResourceArchiveChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug(constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected ResourceArchive construct(ResourceArchive archive) throws ParameterValidateException {
        if (archive.getResourceId() == null) {
            throw new ParameterValidateException("Не указан resourceId");
        }
        archive.setResource(getGovernor(archive).build(archive.getResourceId()));
        return archive;
    }

    private LordOfResources getGovernor(ResourceArchive archive) {
        if (archive == null) {
            throw new ParameterValidateException();
        }
        switch (archive.getResourceType()) {
            case WEBSITE:
                return governorOfWebSite;
            case DATABASE:
                return governorOfDatabase;
            default:
                throw new ParameterValidateException("Неизвестный тип архива");
        }
    }

    @Override
    public ResourceArchive build(String resourceId) throws ResourceNotFoundException {
        if (resourceId == null) {
            throw new ParameterValidateException("Не указан resourceId");
        }
        ResourceArchive archive = repository.findOne(resourceId);
        if (archive == null) {
            throw new ResourceNotFoundException();
        }
        return construct(archive);
    }

    @Override
    public ResourceArchive build(Map<String, String> keyValue) throws ResourceNotFoundException {
        ResourceArchive archive = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            archive = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (archive == null) {
            throw new ResourceNotFoundException();
        }

        return construct(archive);
    }

    @Override
    public Collection<ResourceArchive> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<ResourceArchive> archives = new ArrayList<>();

        if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            for (ResourceArchive archive : repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId"))) {
                archives.add(construct(archive));
            }
        } else if (keyValue.get("accountId") != null) {
            for (ResourceArchive archive : repository.findByAccountId(keyValue.get("accountId"))) {
                archives.add(construct(archive));
            }
        } else if (keyValue.get("serviceId") != null) {
            for (ResourceArchive archive : repository.findByServiceId(keyValue.get("serviceId"))) {
                archives.add(construct(archive));
            }
        }

        return archives;
    }

    @Override
    public Collection<ResourceArchive> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(ResourceArchive archive) {
        repository.save(archive);
    }
}
