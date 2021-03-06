package ru.majordomo.hms.rc.user.test.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Address;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.*;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.*;

import javax.validation.ConstraintViolationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static ru.majordomo.hms.rc.user.resources.PersonType.COMPANY;
import static ru.majordomo.hms.rc.user.resources.PersonType.INDIVIDUAL_FOREIGN;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class
        },
        webEnvironment = NONE
)
public class GovernorOfPersonTest {
    @Autowired
    private GovernorOfPerson governor;

    @Autowired
    private PersonRepository repository;

    private List<Person> persons;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        persons = ResourceGenerator.generateBatchOfPerson();
        persons.get(0).addLinkedAccountId(persons.get(1).getAccountId());

        repository.saveAll(persons);
    }

    @After
    public void cleanUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void createIndividual() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        System.out.println(serviceMessage.toString());
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.setAccountId(null);
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutFirstname() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("firstname");
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutPostalAddress() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("postalAddress");
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        serviceMessage.addParam("emailAddresses", Collections.singletonList("not_email_address"));
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadPhoneNumber() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("phoneNumbers");
        serviceMessage.addParam("phoneNumbers", Collections.singletonList("not_phone_number"));
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void buildAllWithLinked() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", persons.get(1).getAccountId());
        List<Person> buildedPersons = (List<Person>) governor.buildAll(keyValue);
        assertThat(buildedPersons.size(), is(2));
    }

    @Test
    public void buildAllWithoutLinked() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", persons.get(0).getAccountId());
        List<Person> buildedPersons = (List<Person>) governor.buildAll(keyValue);
        assertThat(buildedPersons.size(), is(1));
    }

    @Test
    public void buildByLinkedAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", persons.get(1).getAccountId());
        keyValue.put("resourceId", persons.get(0).getId());
        Person person = governor.build(keyValue);
        assertNotNull(person);
    }

    @Test
    public void manipulateLinkedAccountIds() throws Exception {
        Person person1 = persons.get(0);
        Person person2 = persons.get(1);
        person1.addLinkedAccountId(person2.getAccountId());
        assertThat(person1.getLinkedAccountIds().size(), is(1));

        person1.removeLinkedAccountId(person2.getAccountId());
        assertThat(person1.getLinkedAccountIds().size(), is(0));

        person2.addLinkedAccountId(ObjectId.get().toString());
        assertThat(person2.getLinkedAccountIds().size(), is(1));

        person2.removeLinkedAccountId(ObjectId.get().toString());
        assertThat(person2.getLinkedAccountIds().size(), is(1));
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();

        List<String> newEmailAddresses = Arrays.asList("new1@example.com", "new2@example.com");
        List<String> newPhoneNumbers = Arrays.asList("+79501234567", "+79219876543");
        String newFirstname = "????????????";
        String newLastname = "????????????????";
        String newMiddlename = "????????????????";
        Address newPostalAddress = new Address("195000", "?????????? ??????????????, ?????? ??????????????????????", "??????????-??????????????????");

        Map<String, String> newPassport = new HashMap<>();
        newPassport.put("address", "?????????? ???????????????? ?????? ???? ????????");
        newPassport.put("issuedOrg", "1 ???? ??????????-????????????????????");
        newPassport.put("issuedDate", "1990-01-01");
        newPassport.put("number", "4545454545");
        newPassport.put("birthday", "1990-01-01");

        serviceMessage.setAccountId(persons.get(0).getAccountId());
        serviceMessage.addParam("resourceId", persons.get(0).getId());
        serviceMessage.addParam("firstname", newFirstname);
        serviceMessage.addParam("lastname", newLastname);
        serviceMessage.addParam("middlename", newMiddlename);
        serviceMessage.addParam("emailAddresses", newEmailAddresses);
        serviceMessage.addParam("phoneNumbers", newPhoneNumbers);
        serviceMessage.addParam("postalAddress", objectMapper.convertValue(newPostalAddress, LinkedHashMap.class));
        serviceMessage.addParam("passport", newPassport);

        OperationOversight<Person> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Person gotPerson = repository
                .findById(persons.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("???????????? ???? ????????????"));
        assertThat(gotPerson.getFirstname(), is(newFirstname));
        assertThat(gotPerson.getLastname(), is(newLastname));
        assertThat(gotPerson.getMiddlename(), is(newMiddlename));
        assertThat(gotPerson.getEmailAddresses(), is(newEmailAddresses));
        assertThat(gotPerson.getPhoneNumbers(), is(newPhoneNumbers));
        System.out.println(gotPerson.getPostalAddress());
        System.out.println(newPostalAddress);
        assertEquals(gotPerson.getPostalAddress(), newPostalAddress);
        assertThat(gotPerson.getPassport(), is(governor.buildPassportFromMap(newPassport)));
    }

    @Test
    public void updateFromIndividualToIndividualForeign() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();

        List<String> newEmailAddresses = Arrays.asList("new1@example.com", "new2@example.com");
        List<String> newPhoneNumbers = Arrays.asList("+79501234567", "+79219876543");
        String newFirstname = "Misha";
        String newLastname = "Kolya";
        String newMiddlename = "";
        Address newPostalAddress = new Address("195000", "?????????? ??????????????, ?????? ??????????????????????", "??????????-??????????????????");

        String type = INDIVIDUAL_FOREIGN.name();
        String country = "NL";
        Map<String, String> newPassport = new HashMap<>();
        newPassport.put("address", "?????????? ???????????????? ?????? ???? ????????");
        newPassport.put("issuedOrg", null);
        newPassport.put("number", null);
        newPassport.put("document", "passport 4545454545");
        newPassport.put("birthday", "1990-05-20");

        serviceMessage.setAccountId(persons.get(0).getAccountId());
        serviceMessage.addParam("resourceId", persons.get(0).getId());
        serviceMessage.addParam("firstname", newFirstname);
        serviceMessage.addParam("lastname", newLastname);
        serviceMessage.addParam("middlename", newMiddlename);
        serviceMessage.addParam("type", type);
        serviceMessage.addParam("country", country);
        serviceMessage.addParam("emailAddresses", newEmailAddresses);
        serviceMessage.addParam("phoneNumbers", newPhoneNumbers);
        serviceMessage.addParam("postalAddress", objectMapper.convertValue(newPostalAddress, LinkedHashMap.class));
        serviceMessage.addParam("passport", newPassport);

        OperationOversight<Person> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Person gotPerson = repository
                .findById(persons.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("???????????? ???? ????????????"));
        assertThat(gotPerson.getFirstname(), is(newFirstname));
        assertThat(gotPerson.getLastname(), is(newLastname));
        assertThat(gotPerson.getMiddlename(), is(newMiddlename));
        assertThat(gotPerson.getEmailAddresses(), is(newEmailAddresses));
        assertThat(gotPerson.getPhoneNumbers(), is(newPhoneNumbers));
        System.out.println(gotPerson.getPostalAddress());
        System.out.println(newPostalAddress);
        assertEquals(gotPerson.getPostalAddress(), newPostalAddress);
        assertThat(gotPerson.getPassport(), is(governor.buildPassportFromMap(newPassport)));
    }


    @Test
    public void updateToCompany() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();

        List<String> newEmailAddresses = Arrays.asList("new1@example.com", "new2@example.com");
        List<String> newPhoneNumbers = Arrays.asList("+79501234567", "+79219876543");
        String newOrgName = "????????????";
        String newOrgForm = "??????";
        Address newPostalAddress = new Address("195000", "?????????? ??????????????, ?????? ??????????????????????", "??????????-??????????????????");

        String type = COMPANY.name();
        String country = "RU";
        Map<String, String> newPassport = new HashMap<>();

        Address newLegalAddress = new Address("195001", "?????????? ??????????, ?????? ????????????", "??????????-??????????????????");

        Map<String, Object> newLegalEntity = new HashMap<>();
        newLegalEntity.put("address", objectMapper.convertValue(newLegalAddress, LinkedHashMap.class));
        newLegalEntity.put("inn", "2323232323");
        newLegalEntity.put("ogrn", "0123456789123");
        newLegalEntity.put("kpp", "012345678");
        newLegalEntity.put("directorFirstname", "????????????????");
        newLegalEntity.put("directorLastname", "????????????????????");

        serviceMessage.setAccountId(persons.get(0).getAccountId());
        serviceMessage.addParam("resourceId", persons.get(0).getId());
        serviceMessage.addParam("orgName", newOrgName);
        serviceMessage.addParam("orgForm", newOrgForm);
        serviceMessage.addParam("type", type);
        serviceMessage.addParam("country", country);
        serviceMessage.addParam("emailAddresses", newEmailAddresses);
        serviceMessage.addParam("phoneNumbers", newPhoneNumbers);
        serviceMessage.addParam("postalAddress", objectMapper.convertValue(newPostalAddress, LinkedHashMap.class));
        serviceMessage.addParam("passport", newPassport);
        serviceMessage.addParam("legalEntity", newLegalEntity);

        OperationOversight<Person> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Person gotPerson = repository
                .findById(persons.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("???????????? ???? ????????????"));
        assertThat(gotPerson.getOrgName(), is(newOrgName));
        assertThat(gotPerson.getOrgForm(), is(newOrgForm));
        assertThat(gotPerson.getEmailAddresses(), is(newEmailAddresses));
        assertThat(gotPerson.getPhoneNumbers(), is(newPhoneNumbers));
        System.out.println(gotPerson.getPostalAddress());
        System.out.println(newPostalAddress);
        assertEquals(gotPerson.getPostalAddress(), newPostalAddress);
        assertNull(gotPerson.getPassport());
        assertThat(gotPerson.getLegalEntity(), is(governor.buildLegalEntityFromMap(newLegalEntity)));
    }

    @Test
    public void validateName() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateIndividualPersonCreateServiceMessage();
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", "?????? '????????' ??? 95, +! ????. ????/\"");
        System.out.println(serviceMessage.toString());
        OperationOversight<Person> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = DuplicateKeyException.class)
    public void createTwoPersonWithEqualsAccountIdAndNicHandle() throws Exception {
        List<Person> persons = ResourceGenerator.generateTwoPersonWithEqualsAccountIdAndNicHandle();
        governor.store(persons.get(0));
        governor.store(persons.get(1));
    }
}
