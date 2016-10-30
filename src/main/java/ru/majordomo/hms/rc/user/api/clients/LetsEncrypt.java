package ru.majordomo.hms.rc.user.api.clients;

import ru.majordomo.hms.rc.user.api.interfaces.SSLSigner;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public class LetsEncrypt implements SSLSigner {
    @Override
    public void sign(SSLCertificate sslCertificate) {

    }

    @Override
    public void resign(SSLCertificate sslCertificate) {

    }
}
