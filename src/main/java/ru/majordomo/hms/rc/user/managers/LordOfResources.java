package ru.majordomo.hms.rc.user.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.Serviceable;

public abstract class LordOfResources<T extends Resource> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    public T create(ServiceMessage serviceMessage) throws ParameterValidationException {
        T resource;

        try {
            resource = buildResourceFromServiceMessage(serviceMessage);
            preValidate(resource);
            validate(resource);
            postValidate(resource);
            store(resource);
        } catch (ClassCastException | UnsupportedEncodingException e) {
            throw new ParameterValidationException("Один из параметров указан неверно:" + e.getMessage());
        }

        return resource;
    }

    public abstract T update(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException;

    public abstract void preDelete(String resourceId);

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    public abstract T buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException;

    public abstract void validate(T resource) throws ParameterValidationException;

    public void validateImported(T resource) {}

    public void preValidate(T resource) {}

    public void postValidate(T resource) {}

    public void validateAndStore(T resource) {
        preValidate(resource);
        validate(resource);
        postValidate(resource);
        store(resource);
    }

    public void validateAndStoreImported(T resource) {
        preValidate(resource);
        validateImported(resource);
        postValidate(resource);
        store(resource);
    }

    public void syncWithRedis(T resource) {}

    protected abstract T construct(T resource) throws ParameterValidationException;

    public abstract T build(String resourceId) throws ResourceNotFoundException;

    public abstract T build(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<T> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<T> buildAll();

    public abstract void store(T resource);

    protected Boolean hasResourceIdAndAccountId(Map<String, String> keyValue) {

        boolean byAccountId = false;
        boolean byId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("resourceId")) {
                byId = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        return (byAccountId && byId);
    }

    protected Boolean hasNameAndAccountId(Map<String, String> keyValue) {

        boolean byAccountId = false;
        boolean byName = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("name")) {
                byName = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        return (byAccountId && byName);
    }

    public void setResourceParams(
            T resource,
            ServiceMessage serviceMessage,
            Cleaner cleaner
    ) throws ClassCastException {
        String id = cleaner.cleanString((String) serviceMessage.getParam("id"));
        String accountId = cleaner.cleanString(serviceMessage.getAccountId());
        String name = cleaner.cleanString((String) serviceMessage.getParam("name"));

        if (id != null && !id.equals("")) {
            resource.setId(id);
        }

        if (!(accountId == null || accountId.equals(""))) {
            resource.setAccountId(accountId);
        }

        Object switchedOn = serviceMessage.getParam("switchedOn");
        if (switchedOn != null) {
            resource.setSwitchedOn((Boolean) switchedOn);
        }

        resource.setName(name);
    }

    void preValidateDatabaseServiceId(Serviceable serviceable, StaffResourceControllerClient staffRcClient, String defaultServiceName) {
        if (serviceable.getServiceId() == null || (serviceable.getServiceId().equals(""))) {
            String serverId = staffRcClient.getActiveDatabaseServer().getId();
            List<Service> databaseServices = staffRcClient.getDatabaseServicesByServerId(serverId);
            if (databaseServices != null) {
                for (Service service : databaseServices) {
                    if (service.getServiceTemplate().getServiceType().getName().equals(defaultServiceName)) {
                        serviceable.setServiceId(service.getId());
                        break;
                    }
                }
                if (serviceable.getServiceId() == null || (serviceable.getServiceId().equals(""))) {
                    logger.error("Не найдено serviceType: " + defaultServiceName
                            + " для сервера: " + serverId);
                }
            }
        }

    }
}
