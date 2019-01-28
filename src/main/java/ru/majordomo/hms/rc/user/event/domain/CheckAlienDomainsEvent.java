package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;

public class CheckAlienDomainsEvent extends ApplicationEvent {
    public CheckAlienDomainsEvent() {
        super(0);
    }
}
