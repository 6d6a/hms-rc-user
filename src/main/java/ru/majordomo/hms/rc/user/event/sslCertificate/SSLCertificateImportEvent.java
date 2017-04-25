package ru.majordomo.hms.rc.user.event.sslCertificate;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class SSLCertificateImportEvent extends ResourceImportEvent {
    public SSLCertificateImportEvent(String source) {
        super(source);
    }
}
