package ru.majordomo.hms.rc.user.event.ftpUser;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.FTPUser;

public class FTPUserCreateEvent extends ResourceCreateEvent<FTPUser> {
    public FTPUserCreateEvent(FTPUser source) {
        super(source);
    }
}
