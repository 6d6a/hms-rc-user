package ru.majordomo.hms.rc.user.test.common;

import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.Passport;
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
        serviceMessage.addParam("name", "Климов Никита Анатольевич");
        serviceMessage.addParam("accountId", ObjectId.get().toString());
        serviceMessage.addParam("phoneNumbers", Arrays.asList("+79052033565"));
        serviceMessage.addParam("emailAddresses", Arrays.asList("nikita@klimov.us"));
        serviceMessage.addParam("passport", passportToHashMap(person.getPassport()));
        serviceMessage.addParam("legalEntity", person.getLegalEntity());
        serviceMessage.addParam("country", person.getCountry());
        serviceMessage.addParam("postalAddress", person.getPostalAddress());
        serviceMessage.addParam("owner", person.getOwner());

        return serviceMessage;
    }

    private static HashMap<String, String> passportToHashMap(Passport passport) {
        HashMap<String, String> hashMap = new HashMap<>();
//        hashMap.put("number", passport.getNumber());
//        hashMap.put("issuedOrg", passport.getIssuedOrg());
//        hashMap.put("issuedDate", passport.getIssuedDateAsString());
        hashMap.put("birthday", passport.getBirthdayAsString());
//        hashMap.put("mainPage", passport.getMainPage());
//        hashMap.put("registerPage", passport.getRegisterPage());
//        hashMap.put("address", passport.getAddress());

        return hashMap;
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

    public static ServiceMessage generateUnixAccountCreateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("quota", (long) 1e7);

        return serviceMessage;
    }
}
