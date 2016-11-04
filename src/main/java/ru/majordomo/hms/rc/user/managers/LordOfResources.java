package ru.majordomo.hms.rc.user.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public abstract class LordOfResources {
    protected Logger logger = LoggerFactory.getLogger(LordOfResources.class);
    public abstract Resource create(ServiceMessage serviceMessage) throws ParameterValidateException;

    public abstract Resource update(ServiceMessage serviceMessage) throws ParameterValidateException;

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    protected abstract Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException;

    public abstract void validate(Resource resource) throws ParameterValidateException;

    public abstract Resource build(String resourceId) throws ResourceNotFoundException;

    public abstract Collection<? extends Resource> buildAll();

    public abstract void store(Resource resource);

    public static void setResourceParams(Resource resource,
                                         ServiceMessage serviceMessage,
                                         Cleaner cleaner) throws ClassCastException {
        String id = cleaner.cleanString((String) serviceMessage.getParam("id"));
        String accountId = cleaner.cleanString((String) serviceMessage.getParam("accountId"));
        String name = cleaner.cleanString((String) serviceMessage.getParam("name"));
        Boolean switchedOn = (Boolean) serviceMessage.getParam("switchedOn");

        if (id != null && !id.equals("")) {
            resource.setId(id);
        }

        if (accountId != null || !accountId.equals("")) {
            resource.setAccountId(accountId);
        }

        resource.setName(name);
        resource.setSwitchedOn(switchedOn);

    }


}
