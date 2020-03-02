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

/**
 * Импорт поддоменов из billingdb. Данный сервис используется
 */
@Service
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

    public void pull(String accountId, String serverId) {
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

        return domainRepository.insert(domain);
    }

    public boolean importToMongo(String accountId, String serverId) {
        List<Domain> domains = domainRepository.findByAccountIdAndParentDomainIdNotNull(accountId);

        if (domains != null && !domains.isEmpty()) {
            logger.error("found " + domains.size() + " domains using findByAccountIdAndParentDomainIdNotNull(" + accountId + ")");
            domainRepository.deleteAll(domains);
        }

        pull(accountId, serverId);
        return true;
    }
}
