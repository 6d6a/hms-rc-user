package ru.majordomo.hms.rc.user.event.mailbox;

import org.springframework.context.ApplicationEvent;

public class MailboxImportEvent extends ApplicationEvent {
    public MailboxImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
