package ru.majordomo.hms.rc.user.test.common;

import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.Person;

import static ru.majordomo.hms.rc.user.resources.DBType.*;

public class ServiceMessageGenerator {
    public ServiceMessage generateWebsiteCreateMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
//        serviceMessage.addParam("");
        return serviceMessage;
    }

    public static ServiceMessage generatePersonCreateServiceMessage() {
        List<Person> persons = ResourceGenerator.generateBatchOfPerson();
        Person person = persons.get(0);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", person.getName());
        serviceMessage.addParam("switchedOn", person.getSwitchedOn());
        serviceMessage.addParam("passport", person.getPassport());
        serviceMessage.addParam("legalEntity", person.getLegalEntity());
        serviceMessage.addParam("phoneNumbers", person.getPhoneNumbers());
        serviceMessage.addParam("emailAddresses", person.getEmailAddresses());

        return serviceMessage;
    }

    public static ServiceMessage generatePersonCreateBadServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.addParam("name", "asdf srgkerj asdkfj");
        serviceMessage.addParam("emailAddresses", Arrays.asList("zaborshikov@majordomo.ru"));
        serviceMessage.addParam("phoneNumbers", Arrays.asList("+79522150325"));
        Map<String, String> passport = new HashMap<>();
        passport.put("number", "1234567890");
        passport.put("issuedDate", "2008-03-25");
        passport.put("birthday", "1988-03-25");
        passport.put("country", "RU");
        serviceMessage.addParam("passport", passport);
        serviceMessage.addParam("legalEntity", null);



        return serviceMessage;
    }
}
