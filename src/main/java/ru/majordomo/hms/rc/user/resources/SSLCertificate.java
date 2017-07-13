package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import ru.majordomo.hms.rc.user.resources.validation.UniqueNameResource;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Document(collection = "SSLCertificates")
@UniqueNameResource(SSLCertificate.class)
public class SSLCertificate extends Resource {
    private String dns01Digest;

    private URI challengeLocation;

    private URI authorizationLocation;

    private List<String> dns01Digests = new LinkedList<>();

    private List<URI> challengeLocations = new LinkedList<>();

    private List<URI> authorizationLocations = new LinkedList<>();

    private String key;

    private String csr;

    private String cert;

    private String chain;

    private SSLCertificateState state = SSLCertificateState.NEW;

    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
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

    public List<URI> getChallengeLocations() {
        return challengeLocations;
    }

    public void setChallengeLocations(List<URI> challengeLocations) {
        this.challengeLocations = challengeLocations;
    }

    public List<URI> getAuthorizationLocations() {
        return authorizationLocations;
    }

    public void setAuthorizationLocations(List<URI> authorizationLocations) {
        this.authorizationLocations = authorizationLocations;
    }

    public List<String> getDns01Digests() {
        return dns01Digests;
    }

    public void setDns01Digests(List<String> dns01Digests) {
        this.dns01Digests = dns01Digests;
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

    public List<String> convert(List<URI> source) {
        List<String> converted = new LinkedList<>();

        for(URI uri : source)
            converted.add(uri.toASCIIString());

        return converted;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public String toString() {
        return "SSLCertificate{" +
                ", dns01Digest='" + String.join(";", dns01Digests) + '\'' +
                ", challengeLocation=" + String.join(";", this.convert(challengeLocations)) +
                ", authorizationLocation=" + String.join(";", this.convert(authorizationLocations)) +
                ", key='" + key + '\'' +
                ", csr='" + csr + '\'' +
                ", cert='" + cert + '\'' +
                ", chain='" + chain + '\'' +
                ", state=" + state +
                ", notAfter=" + notAfter +
                "} " + super.toString();
    }
}