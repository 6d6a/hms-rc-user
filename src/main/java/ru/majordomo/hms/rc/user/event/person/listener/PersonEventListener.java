package ru.majordomo.hms.rc.user.event.person.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.person.PersonCreateEvent;
import ru.majordomo.hms.rc.user.event.person.PersonImportEvent;
import ru.majordomo.hms.rc.user.importing.PersonDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

@Component
public class PersonEventListener {
    private final static Logger logger = LoggerFactory.getLogger(PersonEventListener.class);

    private final GovernorOfPerson governorOfPerson;
    private final PersonDBImportService personDBImportService;

    @Autowired
    public PersonEventListener(
            GovernorOfPerson governorOfPerson,
            PersonDBImportService personDBImportService) {
        this.governorOfPerson = governorOfPerson;
        this.personDBImportService = personDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(PersonCreateEvent event) {
        Person person = event.getSource();

        logger.debug("We got CreateEvent");

        try {
            governorOfPerson.validateAndStore(person);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(PersonImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got ImportEvent");

        try {
            personDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
