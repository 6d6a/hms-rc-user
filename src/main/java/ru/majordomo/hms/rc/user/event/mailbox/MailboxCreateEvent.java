package ru.majordomo.hms.rc.user.event.mailbox;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Mailbox;

public class MailboxCreateEvent extends ResourceCreateEvent<Mailbox> {
    public MailboxCreateEvent(Mailbox source) {
        super(source);
    }
}
