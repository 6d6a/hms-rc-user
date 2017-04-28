package ru.majordomo.hms.rc.user.event.domain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.domain.DomainSubDomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainSubDomainImportEvent;
import ru.majordomo.hms.rc.user.importing.DomainSubDomainDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.Domain;

@Component
public class DomainSubDomainEventListener extends ResourceEventListener<Domain> {

    @Autowired
    public DomainSubDomainEventListener(
            GovernorOfDomain governorOfDomain,
            DomainSubDomainDBImportService domainDBImportService) {
        this.governor = governorOfDomain;
        this.dbImportService = domainDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DomainSubDomainCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DomainSubDomainImportEvent event) {
        processImportEvent(event);
    }
}
