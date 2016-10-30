package ru.majordomo.hms.rc.user.managers;

import java.util.Collection;

import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public abstract class LordOfResources {
    public abstract Resource create(ServiceMessage serviceMessage) throws ParameterValidateException;

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    protected abstract Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException;

    public abstract void validate(Resource resource) throws ParameterValidateException;

    public abstract Resource build(String resourceId) throws ResourceNotFoundException;

    public abstract Collection<? extends Resource> buildAll();

    public abstract void store(Resource resource);

    public static void setResourceParams(Resource resource,
                                         ServiceMessage serviceMessage,
                                         Cleaner cleaner) throws ClassCastException {
        String name = cleaner.cleanString((String) serviceMessage.getParam("name"));
        resource.setName(name);
        Boolean switchedOn = (Boolean) serviceMessage.getParam("switchedOn");
        resource.setSwitchedOn(switchedOn);
    }
}
