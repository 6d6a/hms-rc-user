package ru.majordomo.hms.rc.user.event.resourceArchive;

import org.springframework.context.ApplicationEvent;

public class ResourceArchivesCleanEvent extends ApplicationEvent {
    public ResourceArchivesCleanEvent() {
        super("ResourceArchives clean");
    }
}