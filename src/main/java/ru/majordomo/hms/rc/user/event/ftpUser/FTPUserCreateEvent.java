package ru.majordomo.hms.rc.user.event.ftpUser;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.FTPUser;

public class FTPUserCreateEvent extends ApplicationEvent {
    public FTPUserCreateEvent(FTPUser source) {
        super(source);
    }

    @Override
    public FTPUser getSource() {
        return (FTPUser) super.getSource();
    }
}
