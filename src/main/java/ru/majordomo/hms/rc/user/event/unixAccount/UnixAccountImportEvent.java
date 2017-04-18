package ru.majordomo.hms.rc.user.event.unixAccount;

import org.springframework.context.ApplicationEvent;

public class UnixAccountImportEvent extends ApplicationEvent {
    public UnixAccountImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
