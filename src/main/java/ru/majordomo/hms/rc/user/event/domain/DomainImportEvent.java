package ru.majordomo.hms.rc.user.event.domain;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class DomainImportEvent extends ResourceImportEvent {
    public DomainImportEvent(String source) {
        super(source);
    }
}
