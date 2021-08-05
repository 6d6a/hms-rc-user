package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.RegSpec;

import java.util.List;

public class DomainClearSyncEvent extends ApplicationEvent {

    private final List<DomainRegistrar> problemRegistrars;

    public DomainClearSyncEvent(String source, List<DomainRegistrar> problemRegistrars) {
        super(source);
        this.problemRegistrars = problemRegistrars;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public List<DomainRegistrar> getProblemRegistrars() {
        return problemRegistrars;
    }
}