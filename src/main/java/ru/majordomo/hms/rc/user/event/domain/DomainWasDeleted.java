package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.Domain;

public class DomainWasDeleted extends ApplicationEvent {
    public DomainWasDeleted(Domain domain) {
        super(domain);
    }

    @Override
    public Domain getSource() {
        return (Domain) super.getSource();
    }
}
