package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.rc.user.event.domain.DomainsSyncAfterRegisterEvent;
import ru.majordomo.hms.rc.user.event.person.SyncPersonsEvent;
import ru.majordomo.hms.rc.user.event.resourceArchive.ResourceArchivesCleanEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

@RestController
public class SchedulerRestController {
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SchedulerRestController(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scheduler/{scheduleAction}")
    public ResponseEntity<Void> processScheduleAction(@PathVariable(value = "scheduleAction") String scheduleAction) {
        switch (scheduleAction) {
            case "sync_persons":
                publisher.publishEvent(new SyncPersonsEvent());

                break;

            case "ssl_certificates_renew":
                publisher.publishEvent(new SSLCertificatesRenewEvent());

                break;

            case "resource_archives_clean":
                publisher.publishEvent(new ResourceArchivesCleanEvent());

                break;
            case "domains_sync_after_register":
                publisher.publishEvent(new DomainsSyncAfterRegisterEvent());

                break;
            default:
                throw new ParameterValidationException("Неизвестный параметр scheduleAction");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
