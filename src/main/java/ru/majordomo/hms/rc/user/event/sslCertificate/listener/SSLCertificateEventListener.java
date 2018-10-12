package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.rc.user.schedulers.SSLCertificateScheduler;

@Component
@Slf4j
public class SSLCertificateEventListener {

    private SSLCertificateScheduler scheduler;

    @Autowired
    public SSLCertificateEventListener(
            SSLCertificateScheduler scheduler
    ) {
        this.scheduler = scheduler;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SSLCertificatesRenewEvent event) {
        log.debug("We got SSLCertificatesRenewEvent");

        scheduler.renewCerts();
    }
}
