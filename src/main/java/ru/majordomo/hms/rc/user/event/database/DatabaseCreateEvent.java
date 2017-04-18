package ru.majordomo.hms.rc.user.event.database;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.Database;

public class DatabaseCreateEvent extends ApplicationEvent {
    public DatabaseCreateEvent(Database source) {
        super(source);
    }

    @Override
    public Database getSource() {
        return (Database) super.getSource();
    }
}
