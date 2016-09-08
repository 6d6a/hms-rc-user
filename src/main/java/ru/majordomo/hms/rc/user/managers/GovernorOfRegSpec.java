package ru.majordomo.hms.rc.user.managers;

import ru.majordomo.hms.rc.user.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

public class GovernorOfRegSpec extends LordOfResources {
    @Override
    public Resource createResource(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }
}
