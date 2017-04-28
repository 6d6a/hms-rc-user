package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserCreateEvent;
import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserImportEvent;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@Component
public class DatabaseUserDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseUserDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DatabaseUserRepository databaseUserRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DatabaseUserDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            DatabaseUserRepository databaseUserRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.databaseUserRepository = databaseUserRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id, a.name, a.plan_id, a.old_name, p.db, a.server_id, s.jail, s.localdb " +
                "FROM account a " +
                "JOIN servers s ON s.id = a.server_id " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "ORDER BY id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new DatabaseUserImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.name, a.plan_id, a.old_name, p.db, a.server_id, s.jail, s.localdb " +
                "FROM account a " +
                "JOIN servers s ON s.id = a.server_id " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "WHERE a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private DatabaseUser rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found DatabaseUser for id: " + rs.getString("id") + " old_name: " + rs.getString("old_name"));

        String userName = "u" + rs.getString("id");

        String oldName = rs.getString("old_name");

        if (!oldName.equals("")) {
            userName = oldName.replaceAll("\\.", "");
            userName = userName.substring(0, userName.length() > 15 ? 15 : userName.length());
        }

        DatabaseUser databaseUser = new DatabaseUser();
        databaseUser.setAccountId(rs.getString("id"));

        //id of mdb4 mysql_service = '20_mysql_service'
        String serviceId = rs.getString("localdb").equals("1") ? rs.getString("server_id") + "_mysql_service" : "20_mysql_service";

        databaseUser.setServiceId(serviceId);

        //TODO Доставать пароль (Пароли лежат прямо на DB-серверах)
        databaseUser.setPasswordHash("someReallyHardToCrackPasswordHash1111!");
        databaseUser.setType(DBType.MYSQL);
        databaseUser.setSwitchedOn(true);
        databaseUser.setName(userName);

        publisher.publishEvent(new DatabaseUserCreateEvent(databaseUser));

        return null;
    }

    public boolean importToMongo() {
        databaseUserRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<DatabaseUser> databaseUsers = databaseUserRepository.findByAccountId(accountId);

        if (databaseUsers != null && !databaseUsers.isEmpty()) {
            databaseUserRepository.delete(databaseUsers);
        }

        pull(accountId);
        return true;
    }
}
