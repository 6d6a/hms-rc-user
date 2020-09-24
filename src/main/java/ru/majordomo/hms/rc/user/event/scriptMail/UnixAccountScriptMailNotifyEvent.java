package ru.majordomo.hms.rc.user.event.scriptMail;


import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

public class UnixAccountScriptMailNotifyEvent extends ApplicationEvent {

    public UnixAccountScriptMailNotifyEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
