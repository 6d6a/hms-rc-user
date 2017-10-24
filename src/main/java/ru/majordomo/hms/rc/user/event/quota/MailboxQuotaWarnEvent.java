package ru.majordomo.hms.rc.user.event.quota;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.Mailbox;

public class MailboxQuotaWarnEvent extends ApplicationEvent{

    public MailboxQuotaWarnEvent(Mailbox source) {
        super(source);
    }

    @Override
    public Mailbox getSource() {
        return (Mailbox) super.getSource();
    }
}

