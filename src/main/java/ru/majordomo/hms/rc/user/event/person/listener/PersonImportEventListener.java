package ru.majordomo.hms.rc.user.event.person.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.person.PersonCreateEvent;
import ru.majordomo.hms.rc.user.event.person.PersonImportEvent;
import ru.majordomo.hms.rc.user.event.person.PersonSyncEvent;
import ru.majordomo.hms.rc.user.event.person.SyncPersonsEvent;
import ru.majordomo.hms.rc.user.importing.PersonDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.schedulers.PersonScheduler;

@Component
@Profile("import")
public class PersonImportEventListener extends ResourceEventListener<Person> {
    @Autowired
    public PersonImportEventListener(
            GovernorOfPerson governorOfPerson,
            PersonDBImportService personDBImportService
    ) {
        this.governor = governorOfPerson;
        this.dbImportService = personDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(PersonCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(PersonImportEvent event) {
        processImportEvent(event);
    }
}
