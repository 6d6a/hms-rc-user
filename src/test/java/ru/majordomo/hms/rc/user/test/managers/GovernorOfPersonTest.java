package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfPerson;

import java.util.Arrays;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigGovernorOfPerson.class, webEnvironment = NONE)
public class GovernorOfPersonTest {
    @Autowired
    private GovernorOfPerson governor;

    @Test
    public void create() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.setAccountId(null);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("name");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutBadEmail() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("emailAddresses");
        serviceMessage.addParam("emailAddresses", Arrays.asList("not_email_address"));
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutBadPhoneNumber() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateServiceMessage();
        serviceMessage.delParam("phoneNumbers");
        serviceMessage.addParam("phoneNumbers", Arrays.asList("not_phone_number"));
        governor.create(serviceMessage);
    }
}
