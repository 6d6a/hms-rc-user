package ru.majordomo.hms.rc.user.event.mailbox;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Mailbox;

public class MailboxRedisEvent extends ResourceCreateEvent<Mailbox> {
    public MailboxRedisEvent(Mailbox source) {
        super(source);
    }
}
