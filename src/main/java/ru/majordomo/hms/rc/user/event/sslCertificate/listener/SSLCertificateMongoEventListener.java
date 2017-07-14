package ru.majordomo.hms.rc.user.event.sslCertificate.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import java.net.URI;
import java.util.Collections;

@Component
public class SSLCertificateMongoEventListener extends AbstractMongoEventListener<SSLCertificate> {

    @Autowired
    public SSLCertificateMongoEventListener() {
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<SSLCertificate> event) {
        super.onAfterConvert(event);
        SSLCertificate sslCertificate = event.getSource();

        if (sslCertificate.getAuthorizationLocations().isEmpty()) {
            URI authorizationLocation = sslCertificate.getAuthorizationLocation();
            if (authorizationLocation != null) {
                sslCertificate.setAuthorizationLocations(Collections.singletonList(authorizationLocation));
            }
        }

        if (sslCertificate.getChallengeLocations().isEmpty()) {
            URI challengeLocation = sslCertificate.getChallengeLocation();
            if (challengeLocation != null) {
                sslCertificate.setChallengeLocations(Collections.singletonList(challengeLocation));
            }
        }

        if (sslCertificate.getDns01Digests().isEmpty()) {
            String dns01Digest = sslCertificate.getDns01Digest();
            if (dns01Digest != null && !dns01Digest.equals("")) {
                sslCertificate.setDns01Digests(Collections.singletonList(dns01Digest));
            }
        }
    }
}
