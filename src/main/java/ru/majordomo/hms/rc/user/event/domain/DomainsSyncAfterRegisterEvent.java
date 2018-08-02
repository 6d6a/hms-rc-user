package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;

public class DomainsSyncAfterRegisterEvent extends ApplicationEvent {
    public DomainsSyncAfterRegisterEvent() {
        super(0);
    }
}
