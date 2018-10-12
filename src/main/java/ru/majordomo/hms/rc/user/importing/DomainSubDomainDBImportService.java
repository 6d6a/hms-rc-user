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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ru.majordomo.hms.rc.user.event.domain.DomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainImportEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainSubDomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainSubDomainImportEvent;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Service
@Profile("import")
public class DomainSubDomainDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DomainSubDomainDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DomainRepository domainRepository;
    private final SSLCertificateRepository sslCertificateRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DomainSubDomainDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            DomainRepository domainRepository,
            SSLCertificateRepository sslCertificateRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.domainRepository = domainRepository;
        this.sslCertificateRepository = sslCertificateRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM account a " +
                "JOIN domain d ON a.id = d.acc_id " +
                "WHERE 1 " +
                "GROUP BY a.id " +
                "ORDER BY d.acc_id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new DomainSubDomainImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        //Поддомены
        String query = "SELECT v.ServerName as name, d.Domain_name, a.id " +
                "FROM vhosts v " +
                "LEFT JOIN account a ON a.uid = v.uid " +
                "LEFT JOIN domain d ON v.ServerName = d.Domain_name " +
                "WHERE a.id = :accountId AND d.Domain_name IS NULL AND v.ServerName NOT LIKE '%.onparking.ru'";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMapSubDomain
        );
    }

    private Domain rowMapSubDomain(ResultSet rs, int rowNum) throws SQLException {
        String name = rs.getString("name");
        name = java.net.IDN.toUnicode(name);

        logger.debug("Found SubDomain for acc with id: " + rs.getString("id") + " name: " + name);

        Domain domain = new Domain();
        domain.setAccountId(rs.getString("id"));
        domain.setSwitchedOn(true);
        domain.setName(name);
        domain.setAutoRenew(false);
        String[] splitName = name.split("\\.", 2);
        String parentDomainName = splitName[1];

        Domain parentDomain = domainRepository.findByName(parentDomainName);

        logger.debug("Searching for parentDomain " + parentDomain +
                " of SubDomain for acc with id: " + rs.getString("id") +
                " name: " + name + " Found parentDomain: " + parentDomain);

        if (parentDomain != null) {
            domain.setParentDomainId(parentDomain.getId());
        } else {
            logger.error("Not Found parentDomain of SubDomain for acc with id: " + rs.getString("id") + " name: " + name);
        }

        SSLCertificate sslCertificate = sslCertificateRepository.findByNameAndAccountId(
                name,
                rs.getString("id")
        );

        if (sslCertificate != null) {
            domain.setSslCertificateId(sslCertificate.getId());
        }

        publisher.publishEvent(new DomainSubDomainCreateEvent(domain));

        return null;
    }

    public boolean importToMongo() {
        List<Domain> domains = domainRepository.findByParentDomainIdNotNull();

        if (domains != null && !domains.isEmpty()) {
            logger.error("found " + domains.size() + " domains using findByParentDomainIdNotNull()");
            domainRepository.deleteAll(domains);
        }

        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<Domain> domains = domainRepository.findByAccountIdAndParentDomainIdNotNull(accountId);

        if (domains != null && !domains.isEmpty()) {
            logger.error("found " + domains.size() + " domains using findByAccountIdAndParentDomainIdNotNull(" + accountId + ")");
            domainRepository.deleteAll(domains);
        }

        pull(accountId);
        return true;
    }
}
