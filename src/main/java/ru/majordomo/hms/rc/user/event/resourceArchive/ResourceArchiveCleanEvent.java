package ru.majordomo.hms.rc.user.event.resourceArchive;

import org.springframework.context.ApplicationEvent;

public class ResourceArchiveCleanEvent extends ApplicationEvent {
    public ResourceArchiveCleanEvent(String id) {
        super(id);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}