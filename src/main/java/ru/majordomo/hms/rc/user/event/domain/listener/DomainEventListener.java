package ru.majordomo.hms.rc.user.event.domain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.domain.DomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainImportEvent;
import ru.majordomo.hms.rc.user.importing.DomainDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.Domain;

@Component
public class DomainEventListener extends ResourceEventListener<Domain> {

    @Autowired
    public DomainEventListener(
            GovernorOfDomain governorOfDomain,
            DomainDBImportService domainDBImportService) {
        this.governor = governorOfDomain;
        this.dbImportService = domainDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DomainCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DomainImportEvent event) {
        processImportEvent(event);
    }
}
