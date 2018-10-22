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
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountCreateEvent;
import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountImportEvent;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
@Profile("import")
public class UnixAccountDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(UnixAccountDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final UnixAccountRepository unixAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public UnixAccountDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            UnixAccountRepository unixAccountRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.unixAccountRepository = unixAccountRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM account a " +
                "JOIN servers s ON s.id = a.server_id " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "LEFT JOIN user_shell us ON us.uid = a.uid " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new UnixAccountImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.name, a.plan_id, a.server_id, a.homedir, a.quotaused, a.mailquotaused, a.status, a.uid, " +
                "p.db, p.QuotaKB, " +
                "s.name, s.jail, s.localdb, " +
                "us.shell, us.changed, us.pub_key, us.send_mail, " +
                "ds.id as deny_sendmail " +
                "FROM account a " +
                "LEFT JOIN servers s ON s.id = a.server_id " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "LEFT JOIN user_shell us ON us.uid = a.uid " +
                "LEFT JOIN deny_sendmail ds ON ds.uid = a.uid " +
                "WHERE a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private DatabaseUser rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found UnixAccount for id: " + rs.getString("id"));

        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setId("unixAccount_" + rs.getString("id"));
        unixAccount.setAccountId(rs.getString("id"));
        unixAccount.setUid(rs.getInt("uid"));
        Long quota = rs.getLong("QuotaKB");

        String query = "SELECT SUM(`value`) AS extend_quota  FROM `extend` WHERE `acc_id` = :accountId AND usluga = '15'";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", rs.getString("id"));

        quota += namedParameterJdbcTemplate.query(query,
                namedParameters1,
                ((rs1, rowNum1) -> rs1.getLong("extend_quota"))
        ).stream().reduce(0L, Long::sum);

        unixAccount.setQuota(quota);
        Long quotaUsed = rs.getLong("quotaused") + rs.getLong("mailquotaused");
        quotaUsed = quotaUsed < 0 ? 0L : quotaUsed;
        unixAccount.setQuotaUsed(quotaUsed);
        unixAccount.setSendmailAllowed(rs.getString("deny_sendmail") == null);
        unixAccount.setWritable(!rs.getString("server_id").equals("0") && rs.getString("status").equals("1"));
        unixAccount.setSwitchedOn(rs.getString("status").equals("1"));
        unixAccount.setName("u" + rs.getString("uid"));
        unixAccount.setHomeDir(rs.getString("homedir").equals("") ? "/home/" + unixAccount.getName() : rs.getString("homedir"));

        String serverId = rs.getString("server_id").equals("0") ? "web_server_136" : "web_server_" + rs.getString("server_id");
        unixAccount.setServerId(serverId);

        SSHKeyPair sshKeyPair = new SSHKeyPair();
        sshKeyPair.setPublicKey(rs.getString("pub_key"));
        unixAccount.setKeyPair(sshKeyPair);

        List<CronTask> cronTasks = new ArrayList<>();

        query = "SELECT ucc.active, ucc.uid " +
                "FROM users_crontab_conf ucc " +
                "WHERE ucc.uid = :uid";
        namedParameters1 = new MapSqlParameterSource("uid", rs.getString("uid"));

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                ((rs1, rowNum1) -> {
                    if (rs1.getString("active").equals("1")) {
                        String cronQuery = "SELECT uc.id, uc.uid, uc.minute, uc.hour, uc.dayofm, uc.month, uc.dayofw, uc.command, uc.comment " +
                                "FROM users_crontab uc " +
                                "WHERE uc.uid = :uid";
                        MapSqlParameterSource cronNamedParameters1 = new MapSqlParameterSource("uid", rs1.getString("uid"));

                        cronTasks.addAll(namedParameterJdbcTemplate.query(cronQuery,
                                cronNamedParameters1,
                                ((rs2, rowNum2) -> {
                                    String minute = rs2.getString("minute");
                                    String hour = rs2.getString("hour");
                                    String dayofm = rs2.getString("dayofm");
                                    String month = rs2.getString("month");
                                    String dayOfW = rs2.getString("dayofw");

                                    dayOfW = dayOfW != null && (dayOfW.equals("0-7") || dayOfW.equals("0-6")) ? "1-7" : dayOfW;

                                    String command = rs2.getString("command");
                                    String execTime = minute + " " +
                                            hour + " " +
                                            dayofm + " " +
                                            month + " " +
                                            dayOfW;

                                    logger.debug("Cron task found for uid: " + rs1.getString("uid")
                                    + " command: " + command
                                    + " execTime: " + execTime);

                                    CronTask cronTask = new CronTask();
                                    cronTask.setSwitchedOn(true);
                                    cronTask.setCommand(command);

                                    try {
                                        cronTask.setExecTime(execTime);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
//                                    cronTask.setExecTimeDescription(rs2.getString("comment"));

                                    return cronTask;
                                })
                        ));
                    }

                    return  null;
                })
        );

        unixAccount.setCrontab(cronTasks);

        publisher.publishEvent(new UnixAccountCreateEvent(unixAccount));

        return null;
    }

    public boolean importToMongo() {
        unixAccountRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<UnixAccount> unixAccounts = unixAccountRepository.findByAccountId(accountId);

        if (unixAccounts != null && !unixAccounts.isEmpty()) {
            unixAccountRepository.deleteAll(unixAccounts);
        }

        pull(accountId);
        return true;
    }
}
