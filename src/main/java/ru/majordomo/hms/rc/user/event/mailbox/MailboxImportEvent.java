package ru.majordomo.hms.rc.user.event.mailbox;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class MailboxImportEvent extends ResourceImportEvent {
    public MailboxImportEvent(String source) {
        super(source);
    }
}
