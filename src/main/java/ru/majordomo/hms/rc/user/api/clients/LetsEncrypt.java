package ru.majordomo.hms.rc.user.api.clients;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeUnauthorizedException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificateState;

import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class LetsEncrypt {
    private final static Logger logger = LoggerFactory.getLogger(LetsEncrypt.class);

    private InputStream USER_KEY_FILE;

    // Use "acme://letsencrypt.org" for production server
    private static final String LETSENCRYPT_URL = "acme://letsencrypt.org";
//    private static final String LETSENCRYPT_URL = "acme://letsencrypt.org/staging";

    private static final int KEY_SIZE = 2048;

    private KeyPair userKeyPair;

    public static final String LETSENCRYPT_DNS_RECORD_PREFIX = "_acme-challenge.";

    @Autowired
    public LetsEncrypt(ApplicationContext applicationContext) throws IOException {
        Resource resource = applicationContext.getResource("classpath:cert/letsencrypt_user.key");
        USER_KEY_FILE = resource.getInputStream();
        try (InputStreamReader fr = new InputStreamReader(USER_KEY_FILE)) {
            userKeyPair = KeyPairUtils.readKeyPair(fr);
        }
    }

    public SSLCertificate requestCertificateChallenge(SSLCertificate sslCertificate) throws IOException, AcmeException {
        userKeyPair = getUserKeyPair();

        // Create a session for Let's Encrypt
        Session session = getSession();

        // Register a new user
        Registration reg = getRegistration(session);

        // Create a new authorization
        Authorization auth =  sslCertificate.getAuthorizationLocation() != null ? Authorization.bind(session, sslCertificate.getAuthorizationLocation()) : getAuthorization(reg, sslCertificate.getName());

        Dns01Challenge challenge = sslCertificate.getChallengeLocation() != null ? Challenge.bind(session, sslCertificate.getChallengeLocation()) : auth.findChallenge(Dns01Challenge.TYPE);

        if (challenge == null) {
            logger.info("No challenge found for " + sslCertificate.getName());

            throw new AcmeException("No challenge found for domain " + sslCertificate.getName());
        }

        logger.info("Challenge digest " + challenge.getDigest());
        if (sslCertificate.getState() == SSLCertificateState.DNS_UPDATED) {
            // Trigger the challenge
            challenge.trigger();
        }

        sslCertificate.setDns01Digest(challenge.getDigest());
        sslCertificate.setAuthorizationLocation(auth.getLocation());
        sslCertificate.setChallengeLocation(challenge.getLocation());
        sslCertificate.setState(sslCertificate.getState() == SSLCertificateState.DNS_UPDATED ? SSLCertificateState.AWAITING_CONFIRMATION : SSLCertificateState.NEED_DNS_ADDING);

        logger.info("Challenge triggered for " + sslCertificate.getName());

        return sslCertificate;
    }

    public SSLCertificate checkChallenge(SSLCertificate sslCertificate) throws AcmeException, IOException {
        userKeyPair = getUserKeyPair();

        // Create a session for Let's Encrypt
        Session session = getSession();

        // Register a new user
        Registration reg = getRegistration(session);

        Dns01Challenge challenge = Challenge.bind(session, sslCertificate.getChallengeLocation());
        if (challenge == null) {
            logger.info("No challenge found for " + sslCertificate.getName());

            throw new AcmeException("No challenge found for domain " + sslCertificate.getName());
        }

        if (challenge.getStatus() == Status.VALID) {
            logger.info("Challenge is valid for " + sslCertificate.getName());

            sslCertificate.setState(SSLCertificateState.ISSUED);

            KeyPair domainKeyPair = getDomainKeyPair();

            sslCertificate.setKey(toPEMString(domainKeyPair));

            CSRBuilder domainCsr = getDomainCsr(sslCertificate.getName(), domainKeyPair);

            sslCertificate.setCsr(toPEMString(domainCsr.getCSR()));

            Certificate certificate = getCertificate(reg, domainCsr.getEncoded());

            // Download the certificate
            X509Certificate cert = downloadCert(certificate);

            sslCertificate.setCert(toPEMString(cert));

            // Download the certificate chain
            X509Certificate[] chain = downloadChain(certificate);

            sslCertificate.setChain(toPEMString(chain));

            Date notAfterDate = cert.getNotAfter();
            Instant notAfterDateInstant = Instant.ofEpochMilli(notAfterDate.getTime());
            LocalDateTime notAfter = LocalDateTime.ofInstant(notAfterDateInstant, ZoneId.systemDefault());

            sslCertificate.setNotAfter(notAfter);
        } else if (challenge.getStatus() == Status.INVALID) {
            sslCertificate.setState(SSLCertificateState.CHALLENGE_INVALID);
            logger.info("Challenge is invalid for " + sslCertificate.getName());
        } else {
            logger.info("Challenge still not valid for " + sslCertificate.getName() + " current status: " + challenge.getStatus());
        }

        return sslCertificate;
    }

    public SSLCertificate renewCertificate(SSLCertificate sslCertificate) throws IOException, AcmeException {
        userKeyPair = getUserKeyPair();

        // Create a session for Let's Encrypt
        Session session = getSession();

        // Register a new user
        Registration reg = getRegistration(session);

        Certificate certificate = reg.requestCertificate(sslCertificate.getCsr().getBytes());

        // Download the certificate
        X509Certificate cert = downloadCert(certificate);

        sslCertificate.setCert(toPEMString(cert));

        // Download the certificate chain
        X509Certificate[] chain = downloadChain(certificate);

        sslCertificate.setChain(toPEMString(chain));

        Date notAfterDate = cert.getNotAfter();
        Instant notAfterDateInstant = Instant.ofEpochMilli(notAfterDate.getTime());
        LocalDateTime notAfter = LocalDateTime.ofInstant(notAfterDateInstant, ZoneId.systemDefault());

        sslCertificate.setNotAfter(notAfter);

        return sslCertificate;
    }

    private Session getSession() throws IOException {
        // Create a session for Let's Encrypt
        return new Session(LETSENCRYPT_URL, userKeyPair);
    }

    private Registration getRegistration(Session session) throws AcmeException {
        // Register a new user
        Registration reg = null;
        try {
            reg = new RegistrationBuilder().create(session);
            logger.info("Registered a new user, URI: " + reg.getLocation());
        } catch (AcmeConflictException ex) {
            reg = Registration.bind(session, ex.getLocation());
            logger.info("Account does already exist, URI: " + reg.getLocation());
        }

        return reg;
    }

    private KeyPair getUserKeyPair() throws IOException {
        return userKeyPair;
    }

    private KeyPair getDomainKeyPair() throws IOException {
        return KeyPairUtils.createKeyPair(KEY_SIZE);
    }

    private CSRBuilder getDomainCsr(String domain, KeyPair domainKeyPair) throws IOException {
        // Generate a CSR for the domain
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomain(domain);
        csrb.sign(domainKeyPair);

        return csrb;
    }

    private Certificate getCertificate(Registration reg, byte[] domainCsr) throws AcmeException {
        return reg.requestCertificate(domainCsr);
    }

    private X509Certificate downloadCert(Certificate certificate) throws AcmeException {
        return certificate.download();
    }

    private X509Certificate[] downloadChain(Certificate certificate) throws AcmeException {
        return certificate.downloadChain();
    }

    private String toPEMString(Object[] objects) throws IOException {
        StringWriter sw = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sw);
        for (Object object : objects) {
            pemWriter.writeObject(object);
        }

        pemWriter.close();

        return sw.toString();
    }

    private String toPEMString(Object object) throws IOException {
        StringWriter sw = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sw);
        pemWriter.writeObject(object);

        pemWriter.close();

        return sw.toString();
    }

    private Authorization getAuthorization(Registration reg, String domain) throws AcmeException {
        URI agreement = reg.getAgreement();

        Authorization auth = null;
        try {
            auth = reg.authorizeDomain(domain);
            logger.info("Sent authorizeDomain request for " + domain + " auth: " + auth.getLocation());
        } catch (AcmeUnauthorizedException ex) {
            // Maybe there are new T&C to accept?
            reg.modify().setAgreement(agreement).commit();
            // Then try again...
            auth = reg.authorizeDomain(domain);
            logger.info("Accepted agreement and sent authorizeDomain request for " + domain);
        }

        return auth;
    }
}
