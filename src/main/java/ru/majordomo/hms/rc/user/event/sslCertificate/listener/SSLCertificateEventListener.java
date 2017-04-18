package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateCreateEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateImportEvent;
import ru.majordomo.hms.rc.user.importing.SSLCertificateDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Component
public class SSLCertificateEventListener {
    private final static Logger logger = LoggerFactory.getLogger(SSLCertificateEventListener.class);

    private final GovernorOfSSLCertificate governorOfSSLCertificate;
    private final SSLCertificateDBImportService sslCertificateDBImportService;

    @Autowired
    public SSLCertificateEventListener(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            SSLCertificateDBImportService sslCertificateDBImportService) {
        this.governorOfSSLCertificate = governorOfSSLCertificate;
        this.sslCertificateDBImportService = sslCertificateDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSSLCertificateCreateEvent(SSLCertificateCreateEvent event) {
        SSLCertificate sslCertificate = event.getSource();

        logger.debug("We got SSLCertificateCreateEvent");

        try {
            governorOfSSLCertificate.validateAndStore(sslCertificate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSSLCertificateImportEvent(SSLCertificateImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got SSLCertificateImportEvent");

        try {
            sslCertificateDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
