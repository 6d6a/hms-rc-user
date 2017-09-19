package ru.majordomo.hms.rc.user.event.sslCertificate;

import org.springframework.context.ApplicationEvent;

public class SSLCertificatesRenewEvent extends ApplicationEvent {
    public SSLCertificatesRenewEvent() {
        super("SSL Certificates Renew");
    }
}