package ru.majordomo.hms.rc.user.api.interfaces;

import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public interface SSLSigner {
    void sign(SSLCertificate sslCertificate);
    void resign(SSLCertificate sslCertificate);
}
