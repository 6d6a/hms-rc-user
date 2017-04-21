package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.event.domain.DomainCreateEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainImportEvent;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.SSLCertificateRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.RegSpec;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@Service
public class DomainDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DomainDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DomainRepository domainRepository;
    private final SSLCertificateRepository sslCertificateRepository;
    private final ApplicationEventPublisher publisher;

    public static final Map<String, DomainRegistrar> DOMAIN_REGISTRAR_STRING_MAP = new HashMap<String, DomainRegistrar>();

    static {
        DOMAIN_REGISTRAR_STRING_MAP.put("Registrant", DomainRegistrar.NETHOUSE);
        DOMAIN_REGISTRAR_STRING_MAP.put("GPT", DomainRegistrar.R01);
        DOMAIN_REGISTRAR_STRING_MAP.put("RUCENTER", DomainRegistrar.RUCENTER);
        DOMAIN_REGISTRAR_STRING_MAP.put("Enom", DomainRegistrar.ENOM);
        DOMAIN_REGISTRAR_STRING_MAP.put("GoDaddy", DomainRegistrar.GODADDY);
        DOMAIN_REGISTRAR_STRING_MAP.put("Ukrnames", DomainRegistrar.UKRNAMES);
        DOMAIN_REGISTRAR_STRING_MAP.put("RegRu", DomainRegistrar.REGRU);
        DOMAIN_REGISTRAR_STRING_MAP.put("Webnames", DomainRegistrar.WEBNAMES);
    }

    @Autowired
    public DomainDBImportService(
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
        String query = "SELECT d.acc_id " +
                "FROM domain d" +
                "WHERE 1 " +
                "ORDER BY d.acc_id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new DomainImportEvent(resultSet.getString("acc_id")));
        });
    }

    public void pull(String accountId) {
        //Основные домены
        String query = "SELECT d.Domain_name as name, d.acc_id as id, dar.status, dr.source, dr.date_exp " +
                "FROM domain d " +
                "LEFT JOIN domain_auto_renew dar ON d.Domain_name = dar.domain " +
                "LEFT JOIN domain_reg dr ON d.Domain_name = dr.domain " +
                "WHERE d.acc_id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );

        //Алиасы
        query = "SELECT e.Domain_name, e.value as name, e.acc_id as id, dar.status, dr.source, dr.date_exp " +
                "FROM extend e " +
                "LEFT JOIN domain_auto_renew dar ON e.value = dar.domain " +
                "LEFT JOIN domain_reg dr ON dr.domain = e.Domain_name " +
                "WHERE e.usluga = 2  AND e.acc_id = :accountId";
        namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );

        try {
            //Поспим, подождем пока асинхронно в базу не заимпортятся домены
            // (так как по ним нужно искать ParentDomainId)
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        //Поддомены
        query = "SELECT v.ServerName as name, d.Domain_name, a.id " +
                "FROM vhosts v " +
                "LEFT JOIN account a ON a.uid = v.uid " +
                "LEFT JOIN domain d ON v.ServerName = d.Domain_name " +
                "WHERE a.id = :accountId AND d.Domain_name IS NULL AND v.ServerName NOT LIKE '%.onparking.ru'";
        namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMapSubDomain
        );
    }

    private Domain rowMap(ResultSet rs, int rowNum) throws SQLException {
        String name = rs.getString("name");
        name = java.net.IDN.toUnicode(name);

        logger.debug("Found Domain for acc with id: " + rs.getString("id") + " name: " + name);

        Domain domain = new Domain();
        domain.setAccountId(rs.getString("id"));
        domain.setSwitchedOn(true);
        domain.setName(name);
        domain.setAutoRenew(rs.getString("status") != null);

        if (rs.getString("date_exp") != null) {
            RegSpec regSpec = new RegSpec();
            regSpec.setPaidTillAsString(rs.getString("date_exp"));
            regSpec.setRegistrar(DOMAIN_REGISTRAR_STRING_MAP.get(rs.getString("source")));
            domain.setRegSpec(regSpec);
        }

        SSLCertificate sslCertificate = sslCertificateRepository.findByNameAndAccountId(
                name,
                rs.getString("id")
        );

        if (sslCertificate != null) {
            domain.setSslCertificateId(sslCertificate.getId());
        }

        publisher.publishEvent(new DomainCreateEvent(domain));

        return null;
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

        publisher.publishEvent(new DomainCreateEvent(domain));

        return null;
    }

    public boolean importToMongo() {
        domainRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<Domain> domains = domainRepository.findByAccountId(accountId);

        if (domains != null && !domains.isEmpty()) {
            domainRepository.delete(domains);
        }

        pull(accountId);
        return true;
    }
}
