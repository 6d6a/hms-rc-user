package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.net.URI;
import java.time.LocalDateTime;

@Document(collection = "SSLCertificates")
public class SSLCertificate extends Resource {
    private String dns01Digest;

    private URI challengeLocation;

    private URI authorizationLocation;

    private String key;

    private String csr;

    private String cert;

    private String chain;

    private SSLCertificateState state = SSLCertificateState.NEW;

    private LocalDateTime notAfter;

    public URI getChallengeLocation() {
        return challengeLocation;
    }

    public void setChallengeLocation(URI challengeLocation) {
        this.challengeLocation = challengeLocation;
    }

    public URI getAuthorizationLocation() {
        return authorizationLocation;
    }

    public void setAuthorizationLocation(URI authorizationLocation) {
        this.authorizationLocation = authorizationLocation;
    }

    public String getDns01Digest() {
        return dns01Digest;
    }

    public void setDns01Digest(String dns01Digest) {
        this.dns01Digest = dns01Digest;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public SSLCertificateState getState() {
        return state;
    }

    public void setState(SSLCertificateState state) {
        this.state = state;
    }

    public LocalDateTime getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(LocalDateTime notAfter) {
        this.notAfter = notAfter;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public String toString() {
        return "SSLCertificate{" +
                ", dns01Digest='" + dns01Digest + '\'' +
                ", challengeLocation=" + challengeLocation +
                ", authorizationLocation=" + authorizationLocation +
                ", key='" + key + '\'' +
                ", csr='" + csr + '\'' +
                ", cert='" + cert + '\'' +
                ", chain='" + chain + '\'' +
                ", state=" + state +
                ", notAfter=" + notAfter +
                "} " + super.toString();
    }
}