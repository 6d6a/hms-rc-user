package ru.majordomo.hms.rc.user.event.domain;


import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Domain;

public class DomainSubDomainCreateEvent extends ResourceCreateEvent<Domain> {
    public DomainSubDomainCreateEvent(Domain source) {
        super(source);
    }
}
