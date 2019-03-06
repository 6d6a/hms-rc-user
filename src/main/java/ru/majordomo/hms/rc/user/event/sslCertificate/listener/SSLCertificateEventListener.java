package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.event.domain.DomainWasDeleted;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.schedulers.SSLCertificateScheduler;

@Component
@Slf4j
public class SSLCertificateEventListener {

    private SSLCertificateScheduler scheduler;
    private MongoOperations mongoOperations;

    @Autowired
    public SSLCertificateEventListener(
            SSLCertificateScheduler scheduler,
            MongoOperations mongoOperations) {
        this.scheduler = scheduler;
        this.mongoOperations = mongoOperations;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SSLCertificatesRenewEvent event) {
        log.debug("We got SSLCertificatesRenewEvent");

        scheduler.renewCerts();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(DomainWasDeleted event) {
        log.debug("We got {} with source {}", event.getClass(), event.getSource());

        String sslCertificateId = event.getSource().getSslCertificateId();

        if (sslCertificateId != null) {
            boolean sslIsUsing = mongoOperations.exists(
                    new Query(new Criteria("sslCertificateId").is(sslCertificateId)), Domain.class
            );

            if (!sslIsUsing) {
                mongoOperations.remove(
                        new Query(new Criteria("_id").is(sslCertificateId)), SSLCertificate.class
                );

                log.info("SSLCertificate was delete by id {}", sslCertificateId);
            }
        }
    }
}
