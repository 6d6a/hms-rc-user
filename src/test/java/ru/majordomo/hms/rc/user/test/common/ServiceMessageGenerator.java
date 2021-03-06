package ru.majordomo.hms.rc.user.test.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bson.types.ObjectId;

import java.util.*;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;

import static ru.majordomo.hms.rc.user.resources.PersonType.INDIVIDUAL;

public class ServiceMessageGenerator {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public ServiceMessage generateWebsiteCreateMessage() {
        return new ServiceMessage();
    }

    public static ServiceMessage generateIndividualPersonCreateServiceMessage() {
        List<Person> persons = ResourceGenerator.generateBatchOfPerson();
        Person person = persons.get(0);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("type", INDIVIDUAL.name());
        serviceMessage.addParam("firstname", person.getFirstname());
        serviceMessage.addParam("lastname", person.getLastname());
        serviceMessage.addParam("middlename", person.getMiddlename());
        serviceMessage.addParam("phoneNumbers", Collections.singletonList("+79052033565"));
        serviceMessage.addParam("emailAddresses", Collections.singletonList("nikita@klimov.us"));
        serviceMessage.addParam("passport", passportToHashMap(person.getPassport()));
        serviceMessage.addParam("country", person.getCountry());
        serviceMessage.addParam("postalAddress", objectMapper.convertValue(person.getPostalAddress(), LinkedHashMap.class));

        return serviceMessage;
    }

    private static HashMap<String, String> passportToHashMap(Passport passport) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("number", passport.getNumber());
        hashMap.put("issuedOrg", passport.getIssuedOrg());
        hashMap.put("issuedDate", passport.getIssuedDateAsString());
        hashMap.put("birthday", passport.getBirthdayAsString());
//        hashMap.put("mainPage", passport.getMainPage());
//        hashMap.put("registerPage", passport.getRegisterPage());

        return hashMap;
    }

    public static ServiceMessage generatePersonCreateBadServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.addParam("name", "asdf srgkerj asdkfj");
        serviceMessage.addParam("emailAddresses", Collections.singletonList("zaborshikov@majordomo.ru"));
        serviceMessage.addParam("phoneNumbers", Collections.singletonList("+79522150325"));
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
        serviceMessage.addParam("quota", 10485760);

        return serviceMessage;
    }

    public static ServiceMessage generateUnixAccountCreateQuotaStringServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("serverId", ObjectId.get().toString());
        serviceMessage.addParam("quota", "");

        return serviceMessage;
    }

    public static ServiceMessage generateWebsiteCreateServiceMessage(List<String> domainIds, String accountId, String serviceId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(accountId);
        serviceMessage.addParam("applicationServiceId", serviceId);
        serviceMessage.addParam("domainIds", domainIds);
        serviceMessage.addParam("mailEnvelopeFrom", "noreply@whatever.com");

        return serviceMessage;
    }

    public static ServiceMessage generateWebsiteUpdateServiceMessage(List<String> domainIds, String accountId) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(accountId);
        serviceMessage.addParam("domainIds", domainIds);
        serviceMessage.addParam("mailEnvelopeFrom", "noreply@whatever.com");
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
        serviceMessage.addParam("name", "f_-111111");
        serviceMessage.addParam("homedir", "/mjru");
        serviceMessage.addParam("password", "12345678");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserCreateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("name", "databaseUser1");
        serviceMessage.addParam("password", "12345678");
        serviceMessage.addParam("type", "MYSQL");

        return serviceMessage;
    }

    public static ServiceMessage generateDatabaseUserUpdateServiceMessage() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("allowedAddressList", Arrays.asList("8.8.8.8"));

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
