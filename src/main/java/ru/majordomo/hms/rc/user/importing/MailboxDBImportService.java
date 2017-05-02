package ru.majordomo.hms.rc.user.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.rc.user.event.mailbox.MailboxCreateEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxImportEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRedisRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;

@Service
public class MailboxDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(MailboxDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final MailboxRepository mailboxRepository;
    private final GovernorOfMailbox governorOfMailbox;
    private final DomainRepository domainRepository;
    private final ApplicationEventPublisher publisher;
    private final MailboxRedisRepository redisRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public static final Map<String, SpamFilterAction> SPAM_FILTER_ACTION_HASH_MAP = new HashMap<>();
    public static final Map<Integer, SpamFilterMood> SPAM_FILTER_MOOD_HASH_MAP = new HashMap<>();

    static {
        SPAM_FILTER_ACTION_HASH_MAP.put("MARK", SpamFilterAction.MOVE_TO_SPAM_FOLDER);
        SPAM_FILTER_ACTION_HASH_MAP.put("BLOCK", SpamFilterAction.DELETE);

        SPAM_FILTER_MOOD_HASH_MAP.put(80, SpamFilterMood.BAD);
        SPAM_FILTER_MOOD_HASH_MAP.put(69, SpamFilterMood.NEUTRAL);
        SPAM_FILTER_MOOD_HASH_MAP.put(40, SpamFilterMood.GREAT);
    }
    @Autowired
    public MailboxDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            MailboxRepository mailboxRepository,
            GovernorOfMailbox governorOfMailbox,
            DomainRepository domainRepository,
            ApplicationEventPublisher publisher,
            MailboxRedisRepository redisRepository,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.mailboxRepository = mailboxRepository;
        this.governorOfMailbox = governorOfMailbox;
        this.domainRepository = domainRepository;
        this.publisher = publisher;
        this.redisRepository = redisRepository;
        this.redisTemplate = redisTemplate;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM POP3 p " +
                "JOIN domain d ON d.Domain_ID = p.Domain_ID " +
                "JOIN account a ON d.acc_id = a.id " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new MailboxImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.uid, a.popper, " +
                "p.username, p.password, p.filename, p.Domain_ID, p.trash, p.warned, p.blocked, p.quota, " +
                "d.Domain_name, d.smtp, d.avp, d.mailspool, " +
                "ps.score, ps.action, " +
                "pss.name as popper_name " +
                "FROM POP3 p " +
                "JOIN domain d ON d.Domain_ID = p.Domain_ID " +
                "JOIN account a ON d.acc_id = a.id " +
                "JOIN poppers pss ON a.popper=pss.id " +
                "LEFT JOIN pop3_spamprefs ps ON d.Domain_ID =ps.domain_id " +
                "WHERE d.acc_id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private Mailbox rowMap(ResultSet rs, int rowNum) throws SQLException {
        String name = rs.getString("username");

        String originalDomainName = rs.getString("Domain_name");
        String domainName = java.net.IDN.toUnicode(originalDomainName);

//        logger.debug("Found Mailbox for id: " + rs.getString("id") + " name: " + name + "@" + domainName + " originalDomainName: " + originalDomainName);

        Mailbox mailbox = new Mailbox();
        mailbox.setAccountId(rs.getString("id"));
        mailbox.setSwitchedOn(true);
        mailbox.setWritable(true);
        mailbox.setName(name);
        mailbox.setUid(rs.getInt("uid"));
        mailbox.setPasswordHash(rs.getString("password"));
        mailbox.setQuota(rs.getLong("quota") * 1024);
        mailbox.setQuotaUsed(0L);
        mailbox.setMailSpool(rs.getString("mailspool"));
        mailbox.setServerId("mail_server_" + rs.getString("popper"));
        mailbox.setIsAggregator(name.equals("postmaster") && rs.getString("trash").equals("1"));
        mailbox.setMailFromAllowed(rs.getString("smtp").equals("1"));
        mailbox.setAntiSpamEnabled(rs.getString("avp").equals("1"));

        if (rs.getString("action") != null) {
            mailbox.setSpamFilterAction(SPAM_FILTER_ACTION_HASH_MAP.get(rs.getString("action")));
            mailbox.setSpamFilterMood(SPAM_FILTER_MOOD_HASH_MAP.get(rs.getInt("score")));
        }


//        Domain domain = domainRepository.findByNameAndAccountId(
//                domainName,
//                rs.getString("id")
//        );
//
//        if (domain != null) {
//            mailbox.setDomainId(domain.getId());
//            mailbox.setDomain(domain);
//        } else {
//            logger.error("not found domain for mailbox: " + name + "@" + domainName);
//        }

        String query = "SELECT pa.from, pa.to, pa.Domain_name " +
                "FROM POP3_aliases pa " +
                "WHERE pa.Domain_name = :domain_name AND pa.from = :from_name";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("domain_name", domainName)
                .addValue("from_name", name);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                (rs1 -> {
                    List<String> currentRedirectAddresses = mailbox.getRedirectAddresses();

                    String toAdresses = rs1.getString("to");
                    toAdresses = toAdresses.replaceAll(" ", "");
                    String[] adresses = toAdresses.split(",");

                    currentRedirectAddresses.addAll(Arrays.stream(adresses).collect(Collectors.toList()));

                    mailbox.setRedirectAddresses(currentRedirectAddresses);
                })
        );

        query = "SELECT pwbl.wblist_id, pwbl.domain_id, pwbl.email, pwbl.action " +
                "FROM pop3_wblist pwbl " +
                "WHERE pwbl.domain_id = :domain_id";
        namedParameters1 = new MapSqlParameterSource("domain_id", rs.getString("Domain_ID"));

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                (rs1 -> {
                    if (rs1.getString("action").equals("WHITE")) {
                        List<String> currentWhiteList = mailbox.getWhiteList();

                        currentWhiteList.add(rs1.getString("email"));

                        mailbox.setWhiteList(currentWhiteList);
                    } else if (rs1.getString("action").equals("BLACK")) {
                        List<String> currentBlackList = mailbox.getBlackList();

                        currentBlackList.add(rs1.getString("email"));

                        mailbox.setBlackList(currentBlackList);
                    }
                })
        );

        governorOfMailbox.preValidate(mailbox);

        String[] popperName = rs.getString("popper_name").split("\\.");

        //TODO сохранения выключены
//        redisRepository.save(convertMailboxToMailboxForRedis(mailbox, originalDomainName, popperName[0]));
//        saveUserData(mailbox, originalDomainName, popperName[0]);

        if (mailbox.getIsAggregator() != null && mailbox.getIsAggregator() && !originalDomainName.equals(domainName)) {
//            setAggregatorInRedis(mailbox, originalDomainName, popperName[0]);
            logger.error("is Aggregator: " + mailbox.getName() + "@" + domainName + (!originalDomainName.equals(domainName) ? " punycode: " + originalDomainName : ""));
        }
//        publisher.publishEvent(new MailboxCreateEvent(mailbox));

        return null;
    }

    public boolean importToMongo() {
        mailboxRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<Mailbox> mailboxes = mailboxRepository.findByAccountId(accountId);

        if (mailboxes != null && !mailboxes.isEmpty()) {
            mailboxRepository.delete(mailboxes);
        }

        pull(accountId);
        return true;
    }

    private MailboxForRedis convertMailboxToMailboxForRedis(Mailbox mailbox, String domainName, String serverName) {
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        String uidAsString = mailbox.getUid().toString();
        mailboxForRedis.setId(mailbox.getName() + '@' + domainName);
        mailboxForRedis.setName(mailbox.getName() + '@' + domainName);
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackList()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteList()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddresses()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setMailFromAllowed(mailbox.getMailFromAllowed());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        mailboxForRedis.setServerName(serverName);
        mailboxForRedis.setStorageData(uidAsString + ":" + uidAsString + ":" + mailbox.getMailSpool());

        return mailboxForRedis;
    }

    private void saveUserData(Mailbox mailbox, String domainName, String serverName) {
        String uidAsString = mailbox.getUid().toString();
        Map<String, String> userData = new HashMap<>();
        userData.put("uid", uidAsString);
        userData.put("gid", uidAsString);
        userData.put("mail", "maildir:" + mailbox.getMailSpool() + "/" + mailbox.getName());
        userData.put("home", mailbox.getMailSpool() + "/" + mailbox.getName());
        userData.put("host", serverName);
        userData.put("proxy_maybe", "y");
        userData.put("password", mailbox.getPasswordHash());
        ObjectMapper mapper = new ObjectMapper();
        String data = "";
        try {
            data = mapper.writeValueAsString(userData);
        } catch (JsonProcessingException e) {
            logger.error("Mailbox userData не записана в Redis!");
        }
        String key = "mailboxUserData:" + mailbox.getName() + '@' + domainName;
        redisTemplate.boundValueOps(key).set(data);
    }

    private void setAggregatorInRedis(Mailbox mailbox, String domainName, String serverName) {
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        String uidAsString = mailbox.getUid().toString();
        mailboxForRedis.setId("*@" + domainName);
        mailboxForRedis.setName(mailbox.getName() + '@' + domainName);
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackList()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteList()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddresses()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setMailFromAllowed(mailbox.getMailFromAllowed());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        mailboxForRedis.setServerName(serverName);
        mailboxForRedis.setStorageData(uidAsString + ":" + uidAsString + ":" + mailbox.getMailSpool());

        redisRepository.save(mailboxForRedis);
    }
}
