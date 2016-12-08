package ru.majordomo.hms.rc.user.test.common;

import org.bson.types.ObjectId;

import java.util.*;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Domain;
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
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("name", "Климов Никита Анатольевич");
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
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("serverId", ObjectId.get().toString());
        serviceMessage.addParam("quota", (long) 1e7);

        return serviceMessage;
    }

    public static ServiceMessage generateUnixAccountCreateQuotaIntServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("serverId", ObjectId.get().toString());
        serviceMessage.addParam("quota", (int) 10485760);

        return serviceMessage;
    }

    public static ServiceMessage generateUnixAccountCreateQuotaStringServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("serverId", ObjectId.get().toString());
        serviceMessage.addParam("quota", (String) "");

        return serviceMessage;
    }

    public static ServiceMessage generateWebsiteCreateServiceMessage(List<String> domainIds, String accountId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(accountId);
        serviceMessage.addParam("domainIds", domainIds);

        return serviceMessage;
    }

    public static ServiceMessage generateWebsiteUpdateServiceMessage(List<String> domainIds, String accountId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(accountId);
        serviceMessage.addParam("domainIds", domainIds);
        serviceMessage.addParam("cgiEnabled", true);
        List<String> cgiExtensions = new ArrayList<>();
        cgiExtensions.add("py");
        cgiExtensions.add("log");
        serviceMessage.addParam("cgiFileExtensions", cgiExtensions);
        serviceMessage.addParam("allowUrlFopen", true);
        serviceMessage.addParam("mbstringFuncOverload", 4);
        serviceMessage.addParam("followSymLinks", false);
        serviceMessage.addParam("multiViews", true);
        serviceMessage.addParam("accessLogEnabled", false);
        serviceMessage.addParam("autoSubDomains", true);

        return serviceMessage;
    }

    public static ServiceMessage generateWebsiteDeleteServiceMessage(String resourceId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", resourceId);

        return serviceMessage;
    }

    public static ServiceMessage generateMailboxCreateServiceMessage(String domainId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("name", "address1");
        serviceMessage.addParam("domainId", domainId);
        serviceMessage.addParam("password", "qwerty11");

        return serviceMessage;
    }

    public static ServiceMessage generateFTPUserCreateServiceMessageWithoutUnixAccountId() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "f111111");
        serviceMessage.addParam("homedir", "/mjru");
        serviceMessage.addParam("password", "12345678");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserCreateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("password", "12345678");
        serviceMessage.addParam("type", "MYSQL");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserUpdateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("password", "87654321");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserCreateWithoutAccountIdServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("password", "12345678");
        serviceMessage.addParam("type", "MYSQL");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserDeleteServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseCreateServiceMessage(List<String> databaseUserIds) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("databaseUserIds", databaseUserIds);
        serviceMessage.addParam("type", "MYSQL");
        serviceMessage.addParam("name", "database01");

        return serviceMessage;
    }

    public static ServiceMessage generateReportFromTEServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("success", true);

        return serviceMessage;
    }
}
