package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfPerson;

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
        Person person = (Person) governor.create(serviceMessage);
        System.out.println(person);
    }
}
