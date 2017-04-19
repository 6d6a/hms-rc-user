package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;

public class DomainImportEvent extends ApplicationEvent {
    public DomainImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
