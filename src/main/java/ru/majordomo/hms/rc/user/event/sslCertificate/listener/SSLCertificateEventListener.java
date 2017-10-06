package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateCreateEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateImportEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateRenewEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.rc.user.importing.SSLCertificateDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.schedulers.SSLCertificateScheduler;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;

@Component
public class SSLCertificateEventListener extends ResourceEventListener<SSLCertificate> {

    private Sender sender;
    private SSLCertificateScheduler scheduler;

    @Autowired
    public SSLCertificateEventListener(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            SSLCertificateDBImportService sslCertificateDBImportService,
            Sender sender,
            SSLCertificateScheduler scheduler
    ) {
        this.scheduler = scheduler;
        this.governor = governorOfSSLCertificate;
        this.dbImportService = sslCertificateDBImportService;
        this.sender = sender;
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
    public void onRenewEvent(SSLCertificateRenewEvent event) {
        logger.debug("We got SSLCertificateRenewEvent");

        try {

            SSLCertificate sslCertificate = event.getSource();
            if (sslCertificate.getCert() != null && !sslCertificate.getCert().equals("")) {
                X509Certificate myCert = (X509Certificate) CertificateFactory
                        .getInstance("X509")
                        .generateCertificate(
                                // string encoded with default charset
                                new ByteArrayInputStream(sslCertificate.getCert().getBytes())
                        );

                LocalDateTime notAfter = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(myCert.getNotAfter().getTime()), ZoneId.systemDefault());

                if (notAfter.isBefore(LocalDateTime.now().plusDays(5))) {
                    logger.info("Found expiring certificate: " + sslCertificate);
                    ServiceMessage serviceMessage = new ServiceMessage();
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(sslCertificate);
                    serviceMessage.addParam("sslCertificate", json);
                    serviceMessage.setAccountId(sslCertificate.getAccountId());
                    sender.send("ssl-certificate.update", LETSENCRYPT, serviceMessage);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SSLCertificatesRenewEvent event) {
        logger.debug("We got SSLCertificatesRenewEvent");

        scheduler.renewCerts();
    }
}
