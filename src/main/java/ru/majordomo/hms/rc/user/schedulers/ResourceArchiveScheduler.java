package ru.majordomo.hms.rc.user.schedulers;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ru.majordomo.hms.rc.user.event.resourceArchive.ResourceArchiveCleanEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

@Component
public class ResourceArchiveScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GovernorOfResourceArchive governorOfResourceArchive;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ResourceArchiveScheduler(
            GovernorOfResourceArchive governorOfResourceArchive,
            ApplicationEventPublisher publisher
    ) {
        this.governorOfResourceArchive = governorOfResourceArchive;
        this.publisher = publisher;
    }

    @SchedulerLock(name = "cleanResourceArchives")
    public void cleanResourceArchives() {
        logger.info("Started cleanResourceArchives");
        final AtomicInteger archivesCount = new AtomicInteger(0);
        try (Stream<ResourceArchive> stream = governorOfResourceArchive.findByCreatedAtBefore(LocalDateTime.now().minusMonths(3))) {
            stream
                    .peek(archive -> {
                        logger.info("found archive: " + archive);
                        archivesCount.incrementAndGet();
                    })
                    .forEach(archive -> publisher.publishEvent(new ResourceArchiveCleanEvent(archive.getId())));
        }
        logger.info("Ended cleanResourceArchives. Found: " + archivesCount);
    }
}
