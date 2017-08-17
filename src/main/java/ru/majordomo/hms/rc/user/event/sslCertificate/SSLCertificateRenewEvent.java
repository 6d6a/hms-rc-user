package ru.majordomo.hms.rc.user.event.sslCertificate;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public class SSLCertificateRenewEvent extends ResourceCreateEvent<SSLCertificate> {
    public SSLCertificateRenewEvent(SSLCertificate source) {
        super(source);
    }
}