package ru.majordomo.hms.rc.user.schedulers;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Scheduled(cron = "0 20 6,18 * * *")
//    @Scheduled(initialDelay = 10000, fixedDelay = 6000000)
    @SchedulerLock(name = "syncPersons")
    public void cleanTokens() {
        logger.debug("Started syncPersons");
        try (Stream<Person> tokenStream = governorOfPerson.findPersonsWithNicHandlesByNicHandleNotBlank()) {
            tokenStream.forEach(person -> publisher.publishEvent(new PersonSyncEvent(person)));
        }
        logger.debug("Ended syncPersons");
    }
}
