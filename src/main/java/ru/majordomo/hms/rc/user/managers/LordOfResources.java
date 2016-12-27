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

public abstract class LordOfResources {
    protected Logger logger = LoggerFactory.getLogger(LordOfResources.class);
    public abstract Resource create(ServiceMessage serviceMessage) throws ParameterValidateException;

    public abstract Resource update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException;

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    protected abstract Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException;

    public abstract void validate(Resource resource) throws ParameterValidateException;

    protected abstract Resource construct(Resource resource) throws ParameterValidateException;

    public abstract Resource build(String resourceId) throws ResourceNotFoundException;

    public abstract Resource build(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<? extends Resource> buildAll();

    public abstract void store(Resource resource);

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

    public static void setResourceParams(Resource resource,
                                         ServiceMessage serviceMessage,
                                         Cleaner cleaner) throws ClassCastException {
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


}
