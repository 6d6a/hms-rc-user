package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;

public class RegSpecSyncEvent extends ApplicationEvent {

    public RegSpecSyncEvent(String domainName) {
        super(domainName);
    }

    public String getSource() {
        return (String) super.getSource();
    }
}