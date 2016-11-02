package ru.majordomo.hms.rc.user.test.common;

import org.bson.types.ObjectId;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;

import static ru.majordomo.hms.rc.user.resources.DBType.*;

public class ServiceMessageGenerator {
    public static ServiceMessage generateCreateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());

//        serviceMessage.addParam("name", );
        serviceMessage.addParam("type", MYSQL);


        return serviceMessage;
    }
}
