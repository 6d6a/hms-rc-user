package ru.majordomo.hms.rc.user.event.databaseUser;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

public class DatabaseUserCreateEvent extends ResourceCreateEvent<DatabaseUser> {
    public DatabaseUserCreateEvent(DatabaseUser source) {
        super(source);
    }
}
