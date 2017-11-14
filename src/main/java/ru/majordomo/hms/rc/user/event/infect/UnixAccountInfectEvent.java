package ru.majordomo.hms.rc.user.event.infect;


import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.MalwareReport;

public class UnixAccountInfectEvent extends ApplicationEvent {

    public UnixAccountInfectEvent(MalwareReport source) {
        super(source);
    }

    @Override
    public MalwareReport getSource() {
        return (MalwareReport) super.getSource();
    }
}
