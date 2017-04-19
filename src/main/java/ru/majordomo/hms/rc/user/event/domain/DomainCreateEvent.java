package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.Domain;

public class DomainCreateEvent extends ApplicationEvent {
    public DomainCreateEvent(Domain source) {
        super(source);
    }

    @Override
    public Domain getSource() {
        return (Domain) super.getSource();
    }
}
