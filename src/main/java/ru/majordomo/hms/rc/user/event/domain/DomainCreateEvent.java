package ru.majordomo.hms.rc.user.event.domain;


import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Domain;

public class DomainCreateEvent extends ResourceCreateEvent<Domain> {
    public DomainCreateEvent(Domain source) {
        super(source);
    }
}
