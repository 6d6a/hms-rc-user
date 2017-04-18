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
import java.util.stream.Collectors;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.event.database.DatabaseCreateEvent;
import ru.majordomo.hms.rc.user.event.database.DatabaseImportEvent;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@Service
public class DatabaseDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DatabaseRepository databaseRepository;
    private final DatabaseUserRepository databaseUserRepository;
    private final StaffResourceControllerClient staffResourceControllerClient;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DatabaseDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            DatabaseRepository databaseRepository,
            DatabaseUserRepository databaseUserRepository,
            StaffResourceControllerClient staffResourceControllerClient,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.databaseRepository = databaseRepository;
        this.databaseUserRepository = databaseUserRepository;
        this.staffResourceControllerClient = staffResourceControllerClient;
        this.publisher = publisher;
    }

    private void pull() {
        String query = "SELECT a.id, udb.uid, udb.db, udb.host " +
                "FROM account a " +
                "JOIN users_db udb USING(uid) " +
                "ORDER BY id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new DatabaseImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.server_id, udb.uid, udb.db, udb.host, p.QuotaKB " +
                "FROM account a " +
                "JOIN users_db udb USING(uid) " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "WHERE (udb.host = 'mdb4.intr' OR udb.host LIKE 'web%') AND a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private Database rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found Database for id: " + rs.getString("id") +
                " name: " + rs.getString("db") +
                " dbServer: " + rs.getString("host"));

        Database database = new Database();
        database.setAccountId(rs.getString("id"));

        String serverHost = rs.getString("host");
        String serverId;
        if (!serverHost.contains("mdb")) {
            serverId = "web_server_" + rs.getString("server_id");
        } else {
            //id of mdb4 = 'db_server_20'
            serverId = "db_server_20";
        }

        List<ru.majordomo.hms.rc.staff.resources.Service> services = staffResourceControllerClient.getDatabaseServicesByServerId(serverId);

        if(!services.isEmpty()) {
            database.setServiceId(services.get(0).getId());
        }

        database.setType(DBType.MYSQL);
        database.setSwitchedOn(true);
        database.setName(rs.getString("db"));
        database.setQuota(rs.getLong("QuotaKB") * 1024);
        database.setQuotaUsed(0L);
        database.setWritable(true);

        List<DatabaseUser> databaseUsers = databaseUserRepository.findByAccountId(rs.getString("id"));

        database.setDatabaseUserIds(databaseUsers.stream().map(DatabaseUser::getId).collect(Collectors.toList()));

        publisher.publishEvent(new DatabaseCreateEvent(database));

        return null;
    }

    public boolean importToMongo() {
        databaseRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<Database> databases = databaseRepository.findByAccountId(accountId);

        if (databases != null && !databases.isEmpty()) {
            databaseRepository.delete(databases);
        }

        pull(accountId);
        return true;
    }
}
