package ru.majordomo.hms.rc.user.event.unixAccount;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.UnixAccount;

public class UnixAccountCreateEvent extends ApplicationEvent {
    public UnixAccountCreateEvent(UnixAccount source) {
        super(source);
    }

    @Override
    public UnixAccount getSource() {
        return (UnixAccount) super.getSource();
    }
}
