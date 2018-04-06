package ru.majordomo.hms.rc.user.schedulers;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;

@Component
public class SSLCertificateScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GovernorOfSSLCertificate governorOfSSLCertificate;
    private final Sender sender;
    private final GovernorOfDomain governorOfDomain;

    @Autowired
    public SSLCertificateScheduler(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            Sender sender,
            GovernorOfDomain governorOfDomain
    ) {
        this.governorOfSSLCertificate = governorOfSSLCertificate;
        this.sender = sender;
        this.governorOfDomain = governorOfDomain;
    }

    @SchedulerLock(name = "sslCertRenewLock")
    public void renewCerts() {
        logger.info("Started sslCertRenew");

        Integer counter = 0, counterAll = 0;

        try (Stream<SSLCertificate> sslCerts = governorOfSSLCertificate.findAllStream()) {
            List<SSLCertificate> listSSLCerts = sslCerts.collect(Collectors.toList());

            for(SSLCertificate sslCert : listSSLCerts){
                if (this.SSLCertificateProcess(sslCert)) {
                    counter++;
                }
                counterAll++;
                if (counter == 150) {
                    break;
                }
            }

            logger.info("Total Certs processed for renew: " + counter +
                    ". Count of renews before limit exceeds: " + counterAll);
        }
        logger.info("Ended sslCertRenew");
    }

    private boolean SSLCertificateProcess(SSLCertificate sslCertificate) {
        try {

            Domain domain;
            try {
                Map<String, String> buildParams = new HashMap<>();
                buildParams.put("sslCertificateId", sslCertificate.getId());
                domain = governorOfDomain.build(buildParams);
            } catch (ResourceNotFoundException e) {
                logger.info("Found certificate without domain. Id: " + sslCertificate.getId() +
                        " AccountId: " + sslCertificate.getAccountId());
                //SSL сертификат никому не принадлежит -> дропаем
                governorOfSSLCertificate.drop(sslCertificate.getId());
                return false;
            }

            if (sslCertificate.getCert() != null && !sslCertificate.getCert().equals("")) {
                X509Certificate myCert = (X509Certificate) CertificateFactory
                        .getInstance("X509")
                        .generateCertificate(
                                // string encoded with default charset
                                new ByteArrayInputStream(sslCertificate.getCert().getBytes())
                        );

                LocalDateTime notAfter = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(myCert.getNotAfter().getTime()), ZoneId.systemDefault());

                if (domain != null) {
                    //Еcли сертификат выключен и просрочен
                    if (!sslCertificate.isSwitchedOn() && notAfter.isBefore(LocalDateTime.now())) {
                        logger.info("Found NOT active expired certificate. Id: " + sslCertificate.getId() +
                                " name: " + sslCertificate.getName() +
                                " AccountId: " + sslCertificate.getAccountId());

                        domain.setSslCertificateId(null);
                        domain.setSslCertificate(null);
                        governorOfDomain.store(domain);
                        governorOfSSLCertificate.drop(sslCertificate.getId());

                        return false;
                    }

                    //Надо продлить
                    if (sslCertificate.isSwitchedOn() && notAfter.isBefore(LocalDateTime.now().plusDays(5))) {
                        logger.info("Found active expiring certificate. Id: " + sslCertificate.getId() +
                                " name: " + sslCertificate.getName() +
                                " AccountId: " + sslCertificate.getAccountId());

                        ServiceMessage serviceMessage = new ServiceMessage();
                        ObjectMapper mapper = new ObjectMapper();
                        String json = mapper.writeValueAsString(sslCertificate);
                        serviceMessage.addParam("sslCertificate", json);
                        serviceMessage.addParam("resourceId", sslCertificate.getId());
                        serviceMessage.setAccountId(sslCertificate.getAccountId());
                        sender.send(SSL_CERTIFICATE_UPDATE, LETSENCRYPT, serviceMessage);

                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
