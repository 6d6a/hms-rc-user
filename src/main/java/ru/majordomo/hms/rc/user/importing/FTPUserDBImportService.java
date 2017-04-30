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
import java.util.List;

import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserCreateEvent;
import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserImportEvent;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;

@Service
public class FTPUserDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(FTPUserDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final FTPUserRepository ftpUserRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public FTPUserDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            FTPUserRepository ftpUserRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.ftpUserRepository = ftpUserRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id, f.ID, f.Status, f.login, f.password, f.UID, f.HomeDir " +
                "FROM ftp f " +
                "JOIN account a ON f.UID = a.uid " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new FTPUserImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, f.ID, f.Status, f.login, f.password, f.UID, f.HomeDir " +
                "FROM ftp f " +
                "JOIN account a ON f.UID = a.uid " +
                "WHERE a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private FTPUser rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found FTPUser for id: " + rs.getString("id") + " login: " + rs.getString("login"));

        FTPUser ftpUser = new FTPUser();
        ftpUser.setAccountId(rs.getString("id"));
        ftpUser.setPasswordHash(rs.getString("password"));
        ftpUser.setSwitchedOn(rs.getString("Status").equals("1"));
        ftpUser.setName(rs.getString("login"));
        ftpUser.setHomeDir(rs.getString("HomeDir"));
        ftpUser.setUnixAccountId("unixAccount_" + rs.getString("id"));

        String query = "SELECT fa.id, fa.acc_id, fa.remote_ip " +
                "FROM ftp_access fa " +
                "WHERE fa.acc_id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", rs.getString("id"));

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                (rs1 -> {
                    List<String> currentIpsAsCollectionOfString = ftpUser.getAllowedIpsAsCollectionOfString();

                    currentIpsAsCollectionOfString.add(rs1.getString("remote_ip"));

                    ftpUser.setAllowedIpsAsCollectionOfString(currentIpsAsCollectionOfString);
                })
        );

        publisher.publishEvent(new FTPUserCreateEvent(ftpUser));

        return null;
    }

    public boolean importToMongo() {
        ftpUserRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<FTPUser> ftpUsers = ftpUserRepository.findByAccountId(accountId);

        if (ftpUsers != null && !ftpUsers.isEmpty()) {
            ftpUserRepository.delete(ftpUsers);
        }

        pull(accountId);
        return true;
    }
}
