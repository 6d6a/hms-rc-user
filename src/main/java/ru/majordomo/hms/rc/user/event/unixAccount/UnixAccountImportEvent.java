package ru.majordomo.hms.rc.user.event.unixAccount;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class UnixAccountImportEvent extends ResourceImportEvent {
    public UnixAccountImportEvent(String source, String serverId) {
        super(source, serverId);
    }
}
