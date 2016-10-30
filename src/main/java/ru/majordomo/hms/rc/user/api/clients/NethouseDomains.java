package ru.majordomo.hms.rc.user.api.clients;

import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.Domain;

@Service
public class NethouseDomains implements DomainRegistrar {
    @Override
    public void register(Domain domain) {

    }

    @Override
    public void renew(Domain domain) {

    }
}
