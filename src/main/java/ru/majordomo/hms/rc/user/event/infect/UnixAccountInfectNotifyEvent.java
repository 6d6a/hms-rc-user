package ru.majordomo.hms.rc.user.event.infect;


import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.MalwareReport;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

public class UnixAccountInfectNotifyEvent extends ApplicationEvent {

    public UnixAccountInfectNotifyEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
