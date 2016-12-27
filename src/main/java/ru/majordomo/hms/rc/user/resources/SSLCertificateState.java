package ru.majordomo.hms.rc.user.resources;

public enum SSLCertificateState {
    NEW, NEED_DNS_ADDING, AWAITING_DNS_UPDATE, DNS_UPDATED, AWAITING_CONFIRMATION, ISSUED, CHALLENGE_INVALID, NEED_TO_RENEW
}