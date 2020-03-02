package ru.majordomo.hms.rc.user.event.databaseUser;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class DatabaseUserImportEvent extends ResourceImportEvent {
    public DatabaseUserImportEvent(String source, String serverId) {
        super(source, serverId);
    }
}
