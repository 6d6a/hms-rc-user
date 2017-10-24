package ru.majordomo.hms.rc.user.event.quota;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.Mailbox;

public class MailboxQuotaFullEvent extends ApplicationEvent{

    public MailboxQuotaFullEvent(Mailbox source) {
        super(source);
    }

    @Override
    public Mailbox getSource() {
        return (Mailbox) super.getSource();
    }
}

