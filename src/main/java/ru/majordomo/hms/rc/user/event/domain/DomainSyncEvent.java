package ru.majordomo.hms.rc.user.event.domain;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.RegSpec;

public class DomainSyncEvent extends ApplicationEvent {
    private RegSpec regSpec;

    public DomainSyncEvent(String source, RegSpec regSpec) {
        super(source);
        this.regSpec = regSpec;
    }

    public DomainSyncEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public RegSpec getRegSpec() {
        return regSpec;
    }
}