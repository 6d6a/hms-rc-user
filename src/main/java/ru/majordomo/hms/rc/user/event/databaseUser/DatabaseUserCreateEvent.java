package ru.majordomo.hms.rc.user.event.databaseUser;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.DatabaseUser;

public class DatabaseUserCreateEvent extends ApplicationEvent {
    public DatabaseUserCreateEvent(DatabaseUser source) {
        super(source);
    }

    @Override
    public DatabaseUser getSource() {
        return (DatabaseUser) super.getSource();
    }
}
