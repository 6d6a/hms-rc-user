package ru.majordomo.hms.rc.user.event.database;

import org.springframework.context.ApplicationEvent;

public class DatabaseImportEvent extends ApplicationEvent {
    public DatabaseImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
