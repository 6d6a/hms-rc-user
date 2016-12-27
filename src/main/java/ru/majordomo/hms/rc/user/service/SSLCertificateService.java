package ru.majordomo.hms.rc.user.service;

import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.clients.LetsEncrypt;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.repositories.SslCertificateActionIdentityRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.SslCertificateActionIdentity;

import static ru.majordomo.hms.rc.user.api.clients.LetsEncrypt.LETSENCRYPT_DNS_RECORD_PREFIX;

@Service
public class SSLCertificateService {
    private final static Logger logger = LoggerFactory.getLogger(SSLCertificateService.class);

    private final SSLCertificateRepository repository;
    private final LetsEncrypt letsEncrypt;
    private final GovernorOfDomain governorOfDomain;
    private final GovernorOfSSLCertificate governor;
    private final SslCertificateActionIdentityRepository actionIdentityRepository;

    private Sender sender;

    @Autowired
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @Autowired
    public SSLCertificateService(
            SSLCertificateRepository repository,
            LetsEncrypt letsEncrypt,
            GovernorOfDomain governorOfDomain,
            GovernorOfSSLCertificate governor,
            SslCertificateActionIdentityRepository actionIdentityRepository
    ) {
        this.repository = repository;
        this.letsEncrypt = letsEncrypt;
        this.governorOfDomain = governorOfDomain;
        this.actionIdentityRepository = actionIdentityRepository;
        this.governor = governor;
    }

    @Scheduled(fixedDelay = 5000)
    public void processNewSSLCertificates() {
        logger.info("Trying to process NEW SSLCertificates");

        List<SSLCertificate> certificates = repository.findByStateIn(Arrays.asList(SSLCertificateState.NEW, SSLCertificateState.CHALLENGE_INVALID, SSLCertificateState.DNS_UPDATED, SSLCertificateState.NEED_TO_RENEW));

        if (!certificates.isEmpty()) {

            for (SSLCertificate sslCertificate : certificates) {
                try {
                    sslCertificate = letsEncrypt.requestCertificateChallenge(sslCertificate);
                    repository.save(sslCertificate);
                } catch (IOException | AcmeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Scheduled(fixedDelay = 12500)
    public void processAwaitingConfirmationSSLCertificates() {
        logger.info("Trying to process AWAITING_CONFIRMATION SSLCertificates");

        List<SSLCertificate> certificates = repository.findByState(SSLCertificateState.AWAITING_CONFIRMATION);

        if (!certificates.isEmpty()) {
            for (SSLCertificate sslCertificate : certificates) {
                try {
                    sslCertificate = letsEncrypt.checkChallenge(sslCertificate);
                    repository.save(sslCertificate);
                    SslCertificateActionIdentity actionIdentity = actionIdentityRepository.findBySslCertificateId(sslCertificate.getId());
                    if (actionIdentity != null) {
                        ServiceMessage serviceMessage = governor.createSslCertificateServiceMessageForTE(sslCertificate, actionIdentity.getActionIdentity());
                        sender.send("ssl-certificate.create", governor.getTaskExecutorRoutingKeyForSslCertificate(sslCertificate), serviceMessage);
                        actionIdentityRepository.delete(actionIdentity);
                    }
                } catch (IOException | AcmeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Scheduled(fixedDelay = 11500)
    public void processRenewSSLCertificates() {
        logger.info("Trying to process renewing SSLCertificates");

        List<SSLCertificate> certificates = repository.findByState(SSLCertificateState.NEED_TO_RENEW);

        if (!certificates.isEmpty()) {
            for (SSLCertificate sslCertificate : certificates) {
                try {
                    sslCertificate = letsEncrypt.renewCertificate(sslCertificate);
                    repository.save(sslCertificate);
                } catch (IOException | AcmeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Scheduled(fixedDelay = 36000000)
    public void processNeedToRenewSSLCertificates() {
        logger.info("Trying to process NeedToRenew SSLCertificates");

        LocalDateTime dateTime = LocalDateTime.now();
        dateTime = dateTime.plusDays(30);

        List<SSLCertificate> certificates = repository.findByNotAfterLessThan(dateTime);

        if (!certificates.isEmpty()) {
            for (SSLCertificate sslCertificate : certificates) {
                sslCertificate.setState(SSLCertificateState.NEED_TO_RENEW);
                repository.save(sslCertificate);
            }
        }
    }

    @Scheduled(fixedDelay = 7000)
    public void processNeedDnsAddingSSLCertificates() {
        logger.info("Trying to process NeedDnsAdding SSLCertificates");

        List<SSLCertificate> certificates = repository.findByState(SSLCertificateState.NEED_DNS_ADDING);

        if (!certificates.isEmpty()) {
            for (SSLCertificate sslCertificate : certificates) {
                Map<String, String> properties = new HashMap<>();
                properties.put("name", sslCertificate.getName());

                Domain domain = (Domain) governorOfDomain.build(properties);
                if (domain != null) {
                    DNSResourceRecord record = new DNSResourceRecord();
                    record.setRrClass(DNSResourceRecordClass.IN);
                    record.setTtl(300L);
                    record.setRrType(DNSResourceRecordType.TXT);
                    record.setOwnerName(LETSENCRYPT_DNS_RECORD_PREFIX + sslCertificate.getName());
                    record.setData(sslCertificate.getDns01Digest());

                    domain.addDnsResourceRecord(record);

                    governorOfDomain.store(domain);

                    sslCertificate.setState(SSLCertificateState.AWAITING_DNS_UPDATE);
                    repository.save(sslCertificate);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 8000)
    public void processAwaitingDnsUpdateSSLCertificates() {
        logger.info("Trying to process AWAITING_DNS_UPDATE SSLCertificates");

        List<SSLCertificate> certificates = repository.findByState(SSLCertificateState.AWAITING_DNS_UPDATE);

        if (!certificates.isEmpty()) {
            for (SSLCertificate sslCertificate : certificates) {
                try {
                    Lookup lookup = new Lookup(LETSENCRYPT_DNS_RECORD_PREFIX + sslCertificate.getName(), Type.TXT);
                    lookup.setResolver(new SimpleResolver("ns.majordomo.ru"));

                    Record[] records = lookup.run();

                    if (records != null) {
                        for (Record record : records) {
                            TXTRecord txtRecord = (TXTRecord) record;
                            for (Object object : txtRecord.getStrings()) {
                                if (object.toString().equals(sslCertificate.getDns01Digest())) {
                                    logger.info("Correct TXT record found");

                                    sslCertificate.setState(SSLCertificateState.DNS_UPDATED);
                                    repository.save(sslCertificate);
                                    return;
                                }
                            }
                        }
                    }
                } catch (TextParseException | UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}