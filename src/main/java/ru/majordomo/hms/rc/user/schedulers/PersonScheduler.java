package ru.majordomo.hms.rc.user.schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.rc.user.event.person.PersonSyncEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

@Component
public class PersonScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GovernorOfPerson governorOfPerson;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public PersonScheduler(
            GovernorOfPerson governorOfPerson,
            ApplicationEventPublisher publisher
    ) {
        this.governorOfPerson = governorOfPerson;
        this.publisher = publisher;
    }

    public void syncPersons() {
        logger.info("Started syncPersons");
        try (Stream<Person> tokenStream = governorOfPerson.findPersonsWithNicHandlesByNicHandleNotBlank()) {
            tokenStream.forEach(person -> publisher.publishEvent(new PersonSyncEvent(person)));
        }
        logger.info("Ended syncPersons");
    }
}
