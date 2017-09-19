package ru.majordomo.hms.rc.user.event.person;

import org.springframework.context.ApplicationEvent;

public class SyncPersonsEvent extends ApplicationEvent {
    public SyncPersonsEvent() {
        super("Sync Persons");
    }
}