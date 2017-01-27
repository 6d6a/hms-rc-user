package ru.majordomo.hms.rc.user.resources.DTO;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sslCertificateActionIdentities")
public class SslCertificateActionIdentity {
    private String sslCertificateId;
    private String actionIdentity;

    public String getSslCertificateId() {
        return sslCertificateId;
    }

    public void setSslCertificateId(String sslCertificateId) {
        this.sslCertificateId = sslCertificateId;
    }

    public String getActionIdentity() {
        return actionIdentity;
    }

    public void setActionIdentity(String actionIdentity) {
        this.actionIdentity = actionIdentity;
    }
}
