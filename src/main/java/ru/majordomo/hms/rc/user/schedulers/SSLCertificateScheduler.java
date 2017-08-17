package ru.majordomo.hms.rc.user.schedulers;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.rc.user.event.person.PersonSyncEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateRenewEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Component
public class SSLCertificateScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GovernorOfSSLCertificate governorOfSSLCertificate;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SSLCertificateScheduler(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            ApplicationEventPublisher publisher
    ) {
        this.governorOfSSLCertificate = governorOfSSLCertificate;
        this.publisher = publisher;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "sslCertRenewLock")
    public void renewCerts() {
        logger.debug("Started sslCertRenew");
        try (Stream<SSLCertificate> sslCerts = governorOfSSLCertificate.findAllStream()) {
            sslCerts.forEach(sslCert -> publisher.publishEvent(new SSLCertificateRenewEvent(sslCert)));
        }
        logger.debug("Ended sslCertRenew");
    }
}
