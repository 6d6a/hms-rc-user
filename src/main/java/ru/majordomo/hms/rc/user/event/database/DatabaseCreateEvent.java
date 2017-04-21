package ru.majordomo.hms.rc.user.event.database;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Database;

public class DatabaseCreateEvent extends ResourceCreateEvent<Database> {
    public DatabaseCreateEvent(Database source) {
        super(source);
    }
}
