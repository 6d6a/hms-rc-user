package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateCreateEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateImportEvent;
import ru.majordomo.hms.rc.user.importing.SSLCertificateDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Component
@Profile("import")
public class SSLCertificateImportEventListener extends ResourceEventListener<SSLCertificate> {


    @Autowired
    public SSLCertificateImportEventListener(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            SSLCertificateDBImportService sslCertificateDBImportService
    ) {
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
}
