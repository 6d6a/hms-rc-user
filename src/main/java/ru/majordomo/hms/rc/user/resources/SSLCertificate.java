package ru.majordomo.hms.rc.user.resources;

public class SSLCertificate extends Resource {
    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }
}
