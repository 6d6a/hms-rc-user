package ru.majordomo.hms.rc.user.event.databaseUser;

import org.springframework.context.ApplicationEvent;

public class DatabaseUserImportEvent extends ApplicationEvent {
    public DatabaseUserImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
