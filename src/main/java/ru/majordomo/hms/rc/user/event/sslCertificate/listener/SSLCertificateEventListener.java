package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateCreateEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateImportEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.rc.user.importing.SSLCertificateDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.schedulers.SSLCertificateScheduler;

@Component
public class SSLCertificateEventListener extends ResourceEventListener<SSLCertificate> {

    private SSLCertificateScheduler scheduler;

    @Autowired
    public SSLCertificateEventListener(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            SSLCertificateDBImportService sslCertificateDBImportService,
            SSLCertificateScheduler scheduler
    ) {
        this.scheduler = scheduler;
        this.governor = governorOfSSLCertificate;
        this.dbImportService = sslCertificateDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(SSLCertificateCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(SSLCertificateImportEvent event) {
        processImportEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SSLCertificatesRenewEvent event) {
        logger.debug("We got SSLCertificatesRenewEvent");

        scheduler.renewCerts();
    }
}
