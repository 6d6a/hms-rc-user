package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.rc.user.event.person.SyncPersonsEvent;
import ru.majordomo.hms.rc.user.event.resourceArchive.ResourceArchivesCleanEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificatesRenewEvent;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@RestController
public class SchedulerRestController {
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SchedulerRestController(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/{scheduleAction}", method = RequestMethod.POST)
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
            default:
                throw new ParameterValidateException("Неизвестный параметр scheduleAction");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
