package ru.majordomo.hms.rc.user.test.common;

import org.bson.types.ObjectId;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;

import static ru.majordomo.hms.rc.user.resources.DBType.*;

public class ServiceMessageGenerator {
    public ServiceMessage generateWebsiteCreateMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
//        serviceMessage.addParam("");
        return serviceMessage;
    }
}
