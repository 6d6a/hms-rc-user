package ru.majordomo.hms.rc.user.event.sslCertificate;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public class SSLCertificateCreateEvent extends ResourceCreateEvent<SSLCertificate> {
    public SSLCertificateCreateEvent(SSLCertificate source) {
        super(source);
    }
}
