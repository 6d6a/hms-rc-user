package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.ConfigGovernorOfPerson;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigGovernorOfPerson.class)
public class GovernorOfPersonTest {

    List<Person> persons = new ArrayList<>();

    @Autowired
    PersonRepository repository;

    @Autowired
    GovernorOfPerson governor;

    @Before
    public void setUp() {
        persons = ResourceGenerator.generateBatchOfPerson();
        for (Person person: persons) {
            repository.save(person);
        }
    }

    @Test
    public void buildTest() {
        Person standard = persons.get(0);
        Person builded = (Person) governor.build(persons.get(0).getId());
        assertNotNull("Получение персоны не удалось", builded);
        assertThat(builded.getId(), is(standard.getId()));
        assertThat(builded.getName(), is(standard.getName()));
        assertThat(builded.getEmailAddresses(), is(standard.getEmailAddresses()));
        assertThat(builded.getPhoneNumbers(), is(standard.getPhoneNumbers()));
        assertThat(builded.getSwitchedOn(), is(standard.getSwitchedOn()));
        assertThat(builded.getPassport(), is(standard.getPassport()));
        assertThat(builded.getLegalEntity(), is(standard.getLegalEntity()));
    }
}
