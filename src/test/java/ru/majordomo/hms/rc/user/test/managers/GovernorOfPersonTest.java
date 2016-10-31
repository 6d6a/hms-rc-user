package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {GovernorOfPersonTestConfig.class, RepositoriesConfig.class})
public class GovernorOfPersonTest {

//    @Autowired
//    PersonRepository repository;
//
//    @Autowired
//    GovernorOfPerson governor;

    @Before
    public void setUp() {

    }
    @Test
    public void buildTest() {
        Person person = new Person();
        Passport passport = new Passport();
        person.setName("Паровозов Аркадий Локомотивович");
        person.addEmailAddress("arkady@parovozov.ru");
        person.addPhoneNumber("911");
        person.setPassport(passport);
        person.setSwitchedOn(true);

//        repository.save(person);
//
//        Person buildedPerson = (Person) governor.build(person.getId());

        System.out.println(person);
    }
}
