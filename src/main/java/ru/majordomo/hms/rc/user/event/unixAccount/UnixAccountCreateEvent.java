package ru.majordomo.hms.rc.user.event.unixAccount;


import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

public class UnixAccountCreateEvent extends ResourceCreateEvent<UnixAccount> {
    public UnixAccountCreateEvent(UnixAccount source) {
        super(source);
    }
}
