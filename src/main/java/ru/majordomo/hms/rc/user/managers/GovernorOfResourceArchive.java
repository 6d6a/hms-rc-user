package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.repositories.ResourceArchiveRepository;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;
import ru.majordomo.hms.rc.user.resources.Serviceable;
import ru.majordomo.hms.rc.user.resources.validation.group.ResourceArchiveChecks;

import static org.springframework.data.domain.ExampleMatcher.matchingAll;

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
    public ResourceArchive update(ServiceMessage serviceMessage) throws ParameterValidationException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceArchiveId", resourceId);
        keyValue.put("accountId", accountId);

        ResourceArchive resourceArchive = build(keyValue);

        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "switchedOn":
                        resourceArchive.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            resourceArchive.setWillBeDeletedAfter(null);
                        } else {
                            resourceArchive.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            logger.error("ResourceArchive update ClassCastException: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(resourceArchive);
        validate(resourceArchive);
        store(resourceArchive);

        return resourceArchive;
    }

    public void dropByArchivedResourceId(String resourceId) {
        repository.deleteByArchivedResourceId(resourceId);
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
        repository.deleteById(resourceId);
    }

    @Override
    public ResourceArchive buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        ResourceArchive archive = new ResourceArchive();
        setResourceParams(archive, serviceMessage, cleaner);
        String archivedResourceId;
        ResourceArchiveType type = null;

        try {
            archivedResourceId = cleaner.cleanString((String) serviceMessage.getParam("archivedResourceId"));
            if (serviceMessage.getParam("resourceType") != null) {
                for (ResourceArchiveType resourceArchiveType : ResourceArchiveType.values()) {
                    if (resourceArchiveType.name().equals(serviceMessage.getParam("resourceType").toString())) {
                        type = ResourceArchiveType.valueOf(serviceMessage.getParam("resourceType").toString());
                        break;
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        if (archivedResourceId == null) {
            throw new ParameterValidationException("Не указан archivedResourceId");
        } else {
            archive.setArchivedResourceId(archivedResourceId);
        }

        if (type == null) {
            throw new ParameterValidationException("Не указан resourceType");
        } else {
            archive.setResourceType(type);
        }

        Resource resource;

        try {
            LordOfResources<? extends Resource> governor = getGovernor(archive);
            resource = governor.build(archivedResourceId);
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
            throw new ParameterValidationException("Ошибка при поиске сервера");
        }

        if (resource instanceof Serviceable) {
            Serviceable serviceable = (Serviceable) resource;

            archive.setServiceId(serviceable.getServiceId());
        } else {
            throw new ParameterValidationException("Для указанного ресурса не может быть создан архив");
        }

        SecureRandom random = new SecureRandom();
        String dirName = new BigInteger(260, random).toString(32);
        String fileLink = (!archiveHostname.matches("^http[s]?://.*") ? "https://" : "") + archiveHostname + "/" + dirName + "/" + resource.getName() + "." + ResourceArchiveType.FILE_EXTENSION.get(type);
        archive.setFileLink(fileLink);

        return archive;
    }

    @Override
    public void validate(ResourceArchive archive) throws ParameterValidationException {
        Set<ConstraintViolation<ResourceArchive>> constraintViolations = validator.validate(archive, ResourceArchiveChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("archive: " + archive + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected ResourceArchive construct(ResourceArchive archive) throws ParameterValidationException {
        if (archive.getArchivedResourceId() == null) {
            throw new ParameterValidationException("Не указан archivedResourceId");
        }
        archive.setResource(getGovernor(archive).build(archive.getArchivedResourceId()));
        return archive;
    }

    private LordOfResources<? extends Resource> getGovernor(ResourceArchive archive) {
        if (archive == null || archive.getResourceType() == null) {
            throw new ParameterValidationException();
        }
        switch (archive.getResourceType()) {
            case WEBSITE:
                return governorOfWebSite;
            case DATABASE:
                return governorOfDatabase;
            default:
                throw new ParameterValidationException("Неизвестный тип архива");
        }
    }

    @Override
    public ResourceArchive build(String resourceId) throws ResourceNotFoundException {
        if (resourceId == null) {
            throw new ParameterValidationException("Не указан resourceId");
        }
        ResourceArchive archive = repository.findById(resourceId).orElseThrow(() -> new ResourceNotFoundException("Архив не найден"));

        return construct(archive);
    }

    @Override
    public ResourceArchive build(Map<String, String> keyValue) throws ResourceNotFoundException {
        ResourceArchive archive = repository
                .findOne(createResourceArchiveExample(keyValue))
                .orElseThrow(() -> new ResourceNotFoundException("Архив не найден"));

        return construct(archive);
    }

    @Override
    public Collection<ResourceArchive> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        return repository.findAll(createResourceArchiveExample(keyValue))
                .stream()
                .map(this::construct)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ResourceArchive> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(ResourceArchive archive) {
        repository.save(archive);
    }

    public Stream<ResourceArchive> findByWillBeDeletedAfterBefore(LocalDateTime willBeDeletedAfter) {
        return repository.findByWillBeDeletedAfterBefore(willBeDeletedAfter);
    }

    public Count count(Map<String, String> keyValue) {
        return new Count(repository.count(createResourceArchiveExample(keyValue)));
    }

    private Example<ResourceArchive> createResourceArchiveExample(Map<String, String> keyValue) {
        ResourceArchive resourceArchive = new ResourceArchive();

        if (keyValue.get("accountId") != null) {
            resourceArchive.setAccountId(keyValue.get("accountId"));
        }

        if (keyValue.get("archivedResourceId") != null) {
            resourceArchive.setArchivedResourceId(keyValue.get("archivedResourceId"));
        }

        if (keyValue.get("resourceId") != null) {
            resourceArchive.setId(keyValue.get("resourceId"));
        }

        if (keyValue.get("resourceType") != null) {
            resourceArchive.setResourceType(ResourceArchiveType.valueOf(keyValue.get("resourceType")));
        }

        if (keyValue.get("serviceId") != null) {
            resourceArchive.setServiceId(keyValue.get("serviceId"));
        }

        return Example.of(resourceArchive, matchingAll().withIgnorePaths("switchedOn"));
    }
}
