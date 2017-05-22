package ru.majordomo.hms.rc.user.test.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.test.context.web.WebAppConfiguration;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Address;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.*;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import javax.validation.ConstraintViolationException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class
        },
        webEnvironment = NONE)
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

        repository.save(persons);
    }

    @After
    public void cleanUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void create() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.setAccountId(null);
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("name");
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        serviceMessage.addParam("emailAddresses", Collections.singletonList("not_email_address"));
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadPhoneNumber() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("phoneNumbers");
        serviceMessage.addParam("phoneNumbers", Collections.singletonList("not_phone_number"));
        governor.create(serviceMessage);
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
        String newCountry = "NL";
        String newName = "Валера";
        Address newPostalAddress = new Address(195000L, "улица Пушкина, дом Колотушкина", "Санкт-Петербург");

        Map<String, String> newPassport = new HashMap<>();
        newPassport.put("address", "Очень странный дом на горе");
        newPassport.put("issuedOrg", "Google inc.");
        newPassport.put("number", "4545 454545");

        Map<String, String> newLegalEntity = new HashMap<>();
        newLegalEntity.put("address", "Hell");
        newLegalEntity.put("inn", "2323232323");
        newLegalEntity.put("ogrn", "Very unusual number");

        serviceMessage.setAccountId(persons.get(0).getAccountId());
        serviceMessage.addParam("resourceId", persons.get(0).getId());
        serviceMessage.addParam("name", newName);
        serviceMessage.addParam("country", newCountry);
        serviceMessage.addParam("emailAddresses", newEmailAddresses);
        serviceMessage.addParam("phoneNumbers", newPhoneNumbers);
        serviceMessage.addParam("postalAddress", objectMapper.convertValue(newPostalAddress, LinkedHashMap.class));
        serviceMessage.addParam("passport", newPassport);
        serviceMessage.addParam("legalEntity", newLegalEntity);

        governor.update(serviceMessage);

        Person gotPerson = repository.findOne(persons.get(0).getId());
        assertThat(gotPerson.getName(), is(newName));
        assertThat(gotPerson.getCountry(), is(newCountry));
        assertThat(gotPerson.getEmailAddresses(), is(newEmailAddresses));
        assertThat(gotPerson.getPhoneNumbers(), is(newPhoneNumbers));
        System.out.println(gotPerson.getPostalAddress());
        System.out.println(newPostalAddress);
        assertTrue(gotPerson.getPostalAddress().equals(newPostalAddress));
        assertThat(gotPerson.getPassport(), is(governor.buildPassportFromMap(newPassport)));
        assertThat(gotPerson.getLegalEntity(), is(governor.buildLegalEntityFromMap(newLegalEntity)));
    }

    @Test
    public void validateName() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", "НОШ 'ВФЫВ' № 95, +! шк. «»/\"");
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }
}
