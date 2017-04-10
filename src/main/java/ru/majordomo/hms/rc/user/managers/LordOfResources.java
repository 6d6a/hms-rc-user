package ru.majordomo.hms.rc.user.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.resources.Serviceable;

public abstract class LordOfResources<T extends Resource> {
    protected Logger logger = LoggerFactory.getLogger(LordOfResources.class);
    public T create(ServiceMessage serviceMessage) throws ParameterValidateException {
        T resource;

        try {
            resource = buildResourceFromServiceMessage(serviceMessage);
            preValidate(resource);
            validate(resource);
            store(resource);
        } catch (ClassCastException | UnsupportedEncodingException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return resource;
    }

    public abstract T update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException;

    public abstract void preDelete(String resourceId);

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    protected abstract T buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException;

    public abstract void validate(T resource) throws ParameterValidateException;

    public void preValidate(T resource) {}

    protected abstract T construct(T resource) throws ParameterValidateException;

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
        Boolean switchedOn = (Boolean) serviceMessage.getParam("switchedOn");

        if (id != null && !id.equals("")) {
            resource.setId(id);
        }

        if (!(accountId == null || accountId.equals(""))) {
            resource.setAccountId(accountId);
        }

        resource.setName(name);
        resource.setSwitchedOn(switchedOn);
    }

//    public void preValidateServiceId(Serviceable serviceable) {
//        if (databaseUser.getServiceId() == null || (databaseUser.getServiceId().equals(""))) {
//            String serverId = staffRcClient.getActiveDatabaseServer().getId();
//            List<Service> databaseServices = staffRcClient.getDatabaseServicesByServerId(serverId);
//            if (databaseServices != null) {
//                for (Service service : databaseServices) {
//                    if (service.getServiceTemplate().getServiceType().getName().equals(this.defaultServiceName)) {
//                        databaseUser.setServiceId(service.getId());
//                        break;
//                    }
//                }
//                if (databaseUser.getServiceId() == null || (databaseUser.getServiceId().equals(""))) {
//                    logger.error("Не найдено serviceType: " + this.defaultServiceName
//                            + " для сервера: " + serverId);
//                }
//            }
//        }
//    }
}
