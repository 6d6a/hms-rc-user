package ru.majordomo.hms.rc.user.event.sslCertificate;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public class SSLCertificateCreateEvent extends ApplicationEvent {
    public SSLCertificateCreateEvent(SSLCertificate source) {
        super(source);
    }

    @Override
    public SSLCertificate getSource() {
        return (SSLCertificate) super.getSource();
    }
}
