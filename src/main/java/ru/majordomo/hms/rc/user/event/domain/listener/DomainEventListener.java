package ru.majordomo.hms.rc.user.event.domain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.domain.*;
import ru.majordomo.hms.rc.user.importing.DomainDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.RegSpec;

import java.util.List;

@Component
public class DomainEventListener extends ResourceEventListener<Domain> {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public DomainEventListener(
            GovernorOfDomain governorOfDomain,
            DomainDBImportService domainDBImportService,
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
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

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDomainSyncEvent(RegSpecUpdateEvent event) {
        String domainName = event.getSource();

        RegSpec regSpec = event.getRegSpec();

        logger.debug("We got RegSpecUpdateEvent");

        try {
            GovernorOfDomain governorOfDomain = (GovernorOfDomain) governor;
            governorOfDomain.updateRegSpec(domainName, regSpec);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DomainEventListener] Exception: " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDomainClearSyncEvent(DomainClearSyncEvent event) {
        try {
            GovernorOfDomain governorOfDomain = (GovernorOfDomain) governor;
            governorOfDomain.clearNotSyncedDomains();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DomainClearSyncEventListener] Exception: " + e.getMessage());
        }

    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void syncAfterRegister(DomainsSyncAfterRegisterEvent event) {
        logger.debug("We got DomainsSyncAfterRegisterEvent");

        GovernorOfDomain governorOfDomain = (GovernorOfDomain) governor;

        List<String> domainNames = governorOfDomain.findDomainNamesNeedSync();

        domainNames.forEach(domainName -> publisher.publishEvent(new RegSpecSyncEvent(domainName)));

        logger.debug("End of processing DomainsSyncAfterRegisterEvent");
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void syncAfterRegister(RegSpecSyncEvent event) {
        GovernorOfDomain governorOfDomain = (GovernorOfDomain) governor;
        governorOfDomain.syncRegSpec(event.getSource());
    }
}
