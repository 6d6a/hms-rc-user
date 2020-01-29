package ru.majordomo.hms.rc.user.schedulers;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.CertificateHelper;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;

@Slf4j
@Component
public class SSLCertificateScheduler {
    private final GovernorOfSSLCertificate governorOfSSLCertificate;
    private final GovernorOfDomain governorOfDomain;
    private final Sender sender;

    @Autowired
    public SSLCertificateScheduler(
            GovernorOfSSLCertificate governorOfSSLCertificate,
            GovernorOfDomain governorOfDomain,
            Sender sender
    ) {
        this.governorOfSSLCertificate = governorOfSSLCertificate;
        this.governorOfDomain = governorOfDomain;
        this.sender = sender;
    }

    @SchedulerLock(name = "sslCertRenewLock")
    public void renewCerts() {
        log.info("Started sslCertRenew");

        int counter = 0, counterAll = 0;

        try (Stream<SSLCertificate> sslCerts = governorOfSSLCertificate.findAllStream()) {
            List<SSLCertificate> listSSLCerts = sslCerts.collect(Collectors.toList());

            Collections.sort(listSSLCerts);

            for(SSLCertificate sslCert : listSSLCerts){
                if (this.SSLCertificateProcess(sslCert)) {
                    counter++;
                }
                counterAll++;
                if (counter == 150) {
                    break;
                }
            }

            log.info("Total Certs processed for renew: " + counter +
                    ". Count of renews before limit exceeds: " + counterAll);
        }
        log.info("Ended sslCertRenew");
    }

    private boolean SSLCertificateProcess(SSLCertificate sslCertificate) {
        try {
            log.info("start process cert for " + sslCertificate.getName());

            if (sslCertificate.getCert() == null || sslCertificate.getCert().equals("")) {
                log.error("ssl.name: " + sslCertificate.getName() + " ssl.cert is null or empty");
                return false;
            }

            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("accountId", sslCertificate.getAccountId());
            keyValue.put("sslCertificateId", sslCertificate.getId());

            Domain domain = null;
            try {
                domain = governorOfDomain.build(keyValue);
            } catch (ResourceNotFoundException e) {
                log.error("Domain with sslCertificateId {} not found, drop certificate", sslCertificate.getId());
                governorOfSSLCertificate.drop(sslCertificate.getId());
            } catch (Exception e) {
                log.error("Catch e {} e.message {} in build domain by {}; certificate {}",
                        e.getClass(), e.getMessage(), keyValue, sslCertificate);
            }

            if (domain == null) {
                return false;
            }

            LocalDateTime notAfter = CertificateHelper.getNotAfter(sslCertificate);

            log.info("name " + sslCertificate.getName() + " notAfter " + notAfter.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            if (notAfter.isBefore(LocalDateTime.now())) {
                //Еcли сертификат выключен и просрочен
                if (!sslCertificate.isSwitchedOn()) {
                    log.info("Found NOT active expired certificate. Id: " + sslCertificate.getId() +
                            " name: " + sslCertificate.getName() +
                            " AccountId: " + sslCertificate.getAccountId());

                    governorOfSSLCertificate.drop(sslCertificate.getId());

                    return false;
                }
            }

            if (sslCertificate.isSwitchedOn() && notAfter.isBefore(LocalDateTime.now().plusDays(5))) {
                log.info("Found active expiring certificate. Id: " + sslCertificate.getId() +
                        " name: " + sslCertificate.getName() +
                        " AccountId: " + sslCertificate.getAccountId());

                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setAccountId(sslCertificate.getAccountId());
                serviceMessage.addParam("name", sslCertificate.getName());
                sender.send(SSL_CERTIFICATE_UPDATE, LETSENCRYPT, serviceMessage);

                return true;
            }

            log.info("end process cert for " + sslCertificate.getName() + " certificate is not expired");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("e " + e.getClass() + " e.message: " + e.getMessage() + " ssl: " + sslCertificate);
            return false;
        }
    }
}
