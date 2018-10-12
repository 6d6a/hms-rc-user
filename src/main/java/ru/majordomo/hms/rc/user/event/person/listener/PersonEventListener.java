package ru.majordomo.hms.rc.user.event.person.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.event.person.PersonSyncEvent;
import ru.majordomo.hms.rc.user.event.person.SyncPersonsEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.schedulers.PersonScheduler;

@Component
@Slf4j
public class PersonEventListener {
    private final PersonScheduler scheduler;
    private final GovernorOfPerson governorOfPerson;

    @Autowired
    public PersonEventListener(
            GovernorOfPerson governorOfPerson,
            PersonScheduler scheduler
    ) {
        this.scheduler = scheduler;
        this.governorOfPerson = governorOfPerson;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSyncEvent(PersonSyncEvent event) {
        log.debug("We got PersonSyncEvent");

        try {
            governorOfPerson.sync(event.getSource());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SyncPersonsEvent event) {
        log.debug("We got SyncPersonsEvent");

        scheduler.syncPersons();
    }
}
