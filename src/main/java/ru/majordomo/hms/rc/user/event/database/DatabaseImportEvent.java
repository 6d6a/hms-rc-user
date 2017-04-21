package ru.majordomo.hms.rc.user.event.database;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class DatabaseImportEvent extends ResourceImportEvent {
    public DatabaseImportEvent(String source) {
        super(source);
    }
}
