package ru.majordomo.hms.rc.user.event.sslCertificate;

import org.springframework.context.ApplicationEvent;

public class SSLCertificateImportEvent extends ApplicationEvent {
    public SSLCertificateImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
