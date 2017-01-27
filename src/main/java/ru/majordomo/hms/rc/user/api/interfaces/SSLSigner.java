package ru.majordomo.hms.rc.user.api.interfaces;

import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public interface SSLSigner {
    SSLCertificate sign(SSLCertificate sslCertificate);
    SSLCertificate resign(SSLCertificate sslCertificate);
}
