package ru.majordomo.hms.rc.user.event.infect;


import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.MalwareReport;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

public class UnixAccountInfectEvent extends ApplicationEvent {

    public UnixAccountInfectEvent(UnixAccount source) {
        super(source);
    }

    @Override
    public UnixAccount getSource() {
        return (UnixAccount) super.getSource();
    }
}
