package ru.majordomo.hms.rc.user.event.domain.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserCreateEvent;
import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserImportEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseUserDBImportService;
import ru.majordomo.hms.rc.user.importing.DomainDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Domain;

@Component
public class DomainEventListener {
    private final static Logger logger = LoggerFactory.getLogger(DomainEventListener.class);

    private final GovernorOfDomain governorOfDomain;
    private final DomainDBImportService domainDBImportService;

    @Autowired
    public DomainEventListener(
            GovernorOfDomain governorOfDomain,
            DomainDBImportService domainDBImportService) {
        this.governorOfDomain = governorOfDomain;
        this.domainDBImportService = domainDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DomainCreateEvent event) {
        Domain domain = event.getSource();

        logger.debug("We got CreateEvent");

        try {
            governorOfDomain.validateAndStore(domain);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DomainImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got ImportEvent");

        try {
            domainDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
