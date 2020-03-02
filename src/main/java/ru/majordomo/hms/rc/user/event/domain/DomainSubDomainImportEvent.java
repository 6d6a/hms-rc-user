package ru.majordomo.hms.rc.user.event.domain;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class DomainSubDomainImportEvent extends ResourceImportEvent {
    public DomainSubDomainImportEvent(String source) {
        super(source, "");
    }
}
