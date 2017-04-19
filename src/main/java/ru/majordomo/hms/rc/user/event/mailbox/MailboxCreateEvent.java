package ru.majordomo.hms.rc.user.event.mailbox;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.Mailbox;

public class MailboxCreateEvent extends ApplicationEvent {
    public MailboxCreateEvent(Mailbox source) {
        super(source);
    }

    @Override
    public Mailbox getSource() {
        return (Mailbox) super.getSource();
    }
}
