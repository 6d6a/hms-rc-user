package ru.majordomo.hms.rc.user.event.domain;

import ru.majordomo.hms.rc.user.event.ResourceIdEvent;
import ru.majordomo.hms.rc.user.resources.DTO.EntityIdOnly;

/** for dkim */
public class DomainRedisSyncEvent extends ResourceIdEvent {
    public DomainRedisSyncEvent(String DomainId) {
        super(DomainId);
    }

    public DomainRedisSyncEvent(EntityIdOnly domainIdOnly) {
        super(domainIdOnly.getId());
    }
}
