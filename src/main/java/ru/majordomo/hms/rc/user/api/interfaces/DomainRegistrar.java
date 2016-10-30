package ru.majordomo.hms.rc.user.api.interfaces;

import ru.majordomo.hms.rc.user.resources.Domain;

public interface DomainRegistrar {
    void register(Domain domain);
    void renew(Domain domain);
}
