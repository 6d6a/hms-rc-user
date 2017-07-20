package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.RegSpec;

public class DomainClearSyncEvent extends ApplicationEvent {

    public DomainClearSyncEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}