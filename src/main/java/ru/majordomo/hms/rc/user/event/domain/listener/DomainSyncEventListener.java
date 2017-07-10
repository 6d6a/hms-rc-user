package ru.majordomo.hms.rc.user.event.domain.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.domain.DomainSyncEvent;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.RegSpec;

@Component
public class DomainSyncEventListener {
    private final static Logger logger = LoggerFactory.getLogger(DomainSyncEventListener.class);

    private final DomainRepository domainRepository;

    @Autowired
    public DomainSyncEventListener(
            DomainRepository domainRepository
    ) {
        this.domainRepository = domainRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDomainSyncEvent(DomainSyncEvent event) {
        String domainName = event.getSource();

        RegSpec regSpec = event.getRegSpec();

        logger.debug("We got DomainSyncEvent");

        try {
            Domain domain = domainRepository.findByName(domainName);
            if (domain != null) {
                domain.setRegSpec(regSpec);
            }

            domainRepository.save(domain);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DomainSyncEventListener] Exception: " + e.getMessage());
        }
    }
}
