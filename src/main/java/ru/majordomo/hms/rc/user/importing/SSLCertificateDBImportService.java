package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import sun.security.x509.X509CertImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateCreateEvent;
import ru.majordomo.hms.rc.user.event.sslCertificate.SSLCertificateImportEvent;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificateState;

@Service
@Profile("import")
public class SSLCertificateDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(SSLCertificateDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SSLCertificateRepository sslCertificateRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SSLCertificateDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            SSLCertificateRepository sslCertificateRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.sslCertificateRepository = sslCertificateRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM ssl_certificates sc " +
                "JOIN account a ON sc.acc_id = a.id " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new SSLCertificateImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, sc.name, sc.switched_on, sc.dns01Digest, sc.challengeLocation, " +
                "sc.authorizationLocation, sc.key, sc.csr, sc.cert, sc.chain, sc.state " +
                "FROM ssl_certificates sc " +
                "JOIN account a ON sc.acc_id = a.id " +
                "WHERE a.id = :accountId AND sc.state = 'ISSUED'";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private SSLCertificate rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found SSLCertificate for id: " + rs.getString("id") + " name: " + rs.getString("name"));

        SSLCertificate sslCertificate = new SSLCertificate();
        sslCertificate.setAccountId(rs.getString("id"));
        sslCertificate.setSwitchedOn(rs.getString("switched_on").equals("1"));
        sslCertificate.setName(rs.getString("name"));
        //TODO переделать на Lists
        sslCertificate.setDns01Digest(rs.getString("dns01Digest"));

        try {
            sslCertificate.setChallengeLocation(new URI(rs.getString("name")));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            sslCertificate.setAuthorizationLocation(new URI(rs.getString("name")));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        sslCertificate.setKey(rs.getString("key"));
        sslCertificate.setCsr(rs.getString("csr"));
        sslCertificate.setCert(rs.getString("cert"));
        sslCertificate.setChain(rs.getString("chain"));
        sslCertificate.setState(SSLCertificateState.valueOf(rs.getString("state")));

        X509Certificate cert = null;
        try {
            cert = new X509CertImpl(rs.getAsciiStream("cert"));
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        if (cert != null) {
            Date notAfterDate = cert.getNotAfter();
            Instant notAfterDateInstant = Instant.ofEpochMilli(notAfterDate.getTime());
            LocalDateTime notAfter = LocalDateTime.ofInstant(notAfterDateInstant, ZoneId.systemDefault());

            sslCertificate.setNotAfter(notAfter);
        }

        publisher.publishEvent(new SSLCertificateCreateEvent(sslCertificate));

        return null;
    }

    public boolean importToMongo() {
        sslCertificateRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<SSLCertificate> sslCertificates = sslCertificateRepository.findByAccountId(accountId);

        if (sslCertificates != null && !sslCertificates.isEmpty()) {
            sslCertificateRepository.deleteAll(sslCertificates);
        }

        pull(accountId);
        return true;
    }
}
