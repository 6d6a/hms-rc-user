package ru.majordomo.hms.rc.user.event.ftpUser;

import org.springframework.context.ApplicationEvent;

public class FTPUserImportEvent extends ApplicationEvent {
    public FTPUserImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
