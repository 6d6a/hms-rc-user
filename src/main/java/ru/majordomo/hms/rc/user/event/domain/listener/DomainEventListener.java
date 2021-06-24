package ru.majordomo.hms.rc.user.event.domain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.event.domain.*;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxRedisEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.RegSpec;
import ru.majordomo.hms.rc.user.service.AlienDomainsSearcher;

import java.util.List;

@Component
@Slf4j
public class DomainEventListener {
    private final GovernorOfDomain governorOfDomain;
    private final GovernorOfMailbox governorOfMailbox;
    private final ApplicationEventPublisher publisher;
    private final AlienDomainsSearcher alienDomainsSearcher;

    @Autowired
    public DomainEventListener(
            GovernorOfDomain governorOfDomain,
            GovernorOfMailbox governorOfMailbox, ApplicationEventPublisher publisher,
            AlienDomainsSearcher alienDomainsSearcher
    ) {
        this.governorOfMailbox = governorOfMailbox;
        this.publisher = publisher;
        this.governorOfDomain = governorOfDomain;
        this.alienDomainsSearcher = alienDomainsSearcher;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDomainSyncEvent(RegSpecUpdateEvent event) {
        String domainName = event.getSource();

        RegSpec regSpec = event.getRegSpec();

        log.debug("We got RegSpecUpdateEvent");

        try {
            governorOfDomain.updateRegSpec(domainName, regSpec);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[DomainEventListener] Exception: " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDomainClearSyncEvent(DomainClearSyncEvent event) {
        try {
            governorOfDomain.clearNotSyncedDomains();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[DomainClearSyncEventListener] Exception: " + e.getMessage());
        }

    }

    @EventListener
    @Async("redisThreadPoolTaskExecutor")
    public void onMailboxRedisEvent(MailboxRedisEvent event) {
        try {
            Mailbox mailbox = event.getSource();
            mailbox.setDomain(governorOfDomain.build(mailbox.getDomainId()));
            governorOfMailbox.syncWithRedis(mailbox);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[DomainMailboxRedisEvent] Exception: " + e.getMessage());
        }

    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void syncAfterRegister(DomainsSyncAfterRegisterEvent event) {
        log.debug("We got DomainsSyncAfterRegisterEvent");

        List<String> domainNames = governorOfDomain.findDomainNamesNeedSync();

        domainNames.forEach(domainName -> publisher.publishEvent(new RegSpecSyncEvent(domainName)));

        log.debug("End of processing DomainsSyncAfterRegisterEvent");
    }

    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void on(CheckAlienDomainsEvent event) {
        log.debug("We got CheckAlienDomainsEvent");

        alienDomainsSearcher.search();

        log.debug("End of processing CheckAlienDomainsEvent");
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void syncAfterRegister(RegSpecSyncEvent event) {
        governorOfDomain.syncRegSpec(event.getSource());
    }
}
