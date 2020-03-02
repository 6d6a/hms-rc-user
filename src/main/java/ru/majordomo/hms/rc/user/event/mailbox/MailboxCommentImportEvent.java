package ru.majordomo.hms.rc.user.event.mailbox;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class MailboxCommentImportEvent extends ResourceImportEvent {
    public MailboxCommentImportEvent(String source) {
        super(source, "");
    }
}
