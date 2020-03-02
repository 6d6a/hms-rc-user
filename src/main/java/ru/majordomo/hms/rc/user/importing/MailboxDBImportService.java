package ru.majordomo.hms.rc.user.importing;

import org.apache.commons.lang.StringUtils;
import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;

import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.SpamFilterAction;
import ru.majordomo.hms.rc.user.resources.SpamFilterMood;
import ru.majordomo.hms.rc.user.resources.validation.validator.EmailOrDomainValidator;
import ru.majordomo.hms.rc.user.resources.validation.validator.EmailValidator;

import javax.annotation.Nullable;

/**
 * Импорт почтовых ящиков из billingdb.
 * Только записывает в базу данных, сами ящики на почтовом сервере не создаются.
 * Перед его запуском должны быть импортированы домены.
 */
@Service
public class MailboxDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(MailboxDBImportService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MailboxRepository mailboxRepository;
    private final GovernorOfMailbox governorOfMailbox;
    private final ApplicationEventPublisher publisher;
    private final DomainRepository domainRepository;

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
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
            MailboxRepository mailboxRepository,
            GovernorOfMailbox governorOfMailbox,
            ApplicationEventPublisher publisher,
            DomainRepository domainRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailboxRepository = mailboxRepository;
        this.governorOfMailbox = governorOfMailbox;
        this.publisher = publisher;
        this.domainRepository = domainRepository;
    }

    public void pull(String accountId, boolean switchedOn, boolean allowAntispam) {
        String query = "SELECT a.id, a.uid, a.popper, " +
                "p.username, p.password, p.filename, p.Domain_ID, p.trash, p.warned, p.blocked, p.quota, " +
                "d.Domain_name, d.smtp, d.avp, d.mailspool, " +
                "ps.score, ps.action, " +
                "pss.name as popper_name, c.comment " +
                "FROM POP3 p " +
                "JOIN domain d ON d.Domain_ID = p.Domain_ID " +
                "JOIN account a ON d.acc_id = a.id " +
                "JOIN poppers pss ON a.popper=pss.id " +
                "LEFT JOIN pop3_spamprefs ps ON d.Domain_ID =ps.domain_id " +
                "LEFT JOIN POP3_comments c ON c.pop3_id=p.id " +
                "WHERE d.acc_id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        jdbcTemplate.query(query,
                namedParameters1,
                (rs, rowNum) -> rowMap(rs, rowNum, switchedOn, allowAntispam)
        );
    }

    private boolean isValidEmail(String email) {
        return (new EmailValidator()).isValid(email, null);
    }

    private boolean isValidEmailOrDomain(String emailOrDomain) {
        return (new EmailOrDomainValidator()).isValid(emailOrDomain, null);
    }

    private Mailbox rowMap(ResultSet rs, int rowNum, boolean switchedOn, boolean allowAntispam) throws SQLException {
        String name = rs.getString("username");
        String accountId = rs.getString("id");
        long quotaKb = rs.getLong("quota");
        int uid = rs.getInt("uid");
        String popper = rs.getString("popper");
        String mailspool =  rs.getString("mailspool");
        boolean smtpAllowed = "1".equals(rs.getString("smtp"));
        boolean avpAntispam = "1".equals(rs.getString("avp"));
        boolean trash = "1".equals(rs.getString("trash"));
        @Nullable String commentRaw = rs.getString("comment");
        @Nullable String action = rs.getString("action");
        int score = rs.getInt("score");
        String originalDomainName = rs.getString("Domain_name");
        String domainId = rs.getString("Domain_ID");
        String passwordHash = rs.getString("password");


        String mailServerId = "mail_server_" + popper;
        String domainName = java.net.IDN.toUnicode(originalDomainName);

        logger.debug("Found Mailbox for id: " + rs.getString("id") + " name: " + name + "@" + domainName + " originalDomainName: " + originalDomainName);

        Mailbox mailbox = new Mailbox();
        mailbox.setAccountId(accountId);
        mailbox.setSwitchedOn(switchedOn);
        mailbox.setWritable(switchedOn);
        mailbox.setMailFromAllowed(switchedOn && smtpAllowed);
        mailbox.setName(name);

        // uid у юникс аккаунта и ящика может отличаться. Это корректно.
        mailbox.setUid(uid);

        mailbox.setPasswordHash(passwordHash);
        mailbox.setQuota(quotaKb * 1024);
        mailbox.setQuotaUsed(0L);
        mailbox.setMailSpool(mailspool);
        mailbox.setServerId(mailServerId);
        mailbox.setIsAggregator(name.equals("postmaster") && trash);

        try {
            if (StringUtils.isNotEmpty(commentRaw)) {
                String comment = EncodingUtils.getString(commentRaw.getBytes("windows-1251"), "koi8-r");
                mailbox.setComment(comment.length() > 128 ? comment.substring(0, 127) : comment);
            }
        } catch (UnsupportedEncodingException ignore) {}

        if (allowAntispam) {
            mailbox.setAntiSpamEnabled(avpAntispam);
            if (action != null) {
                mailbox.setSpamFilterAction(SPAM_FILTER_ACTION_HASH_MAP.get(action));
                mailbox.setSpamFilterMood(SPAM_FILTER_MOOD_HASH_MAP.get(score));
            }
        }

        Domain domain = domainRepository.findByNameAndAccountId(domainName, accountId);

        if (domain != null) {
            mailbox.setDomainId(domain.getId());
            mailbox.setDomain(domain);
        } else {
            throw new ResourceNotFoundException("Не удалось найти домен для почтового ящика: " + name + "@" + domainName);
        }

        String query = "SELECT pa.from, pa.to, pa.Domain_name FROM POP3_aliases pa WHERE pa.Domain_name = :domain_name AND pa.from = :from_name";
        SqlParameterSource sqlParam = new MapSqlParameterSource("domain_name", domainName)
                .addValue("from_name", name);

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, sqlParam);
        if (rowSet.next()) {
            String toAdressesStr = rowSet.getString("to");
            if (StringUtils.isNotEmpty(toAdressesStr)) {
                String[] toAddreses = toAdressesStr.trim().split("\\s*,\\s*");
                List<String> redirects = Arrays.stream(toAddreses).filter(this::isValidEmail).collect(Collectors.toList());
                mailbox.setRedirectAddresses(redirects);
            }
        }

        query = "SELECT pwbl.wblist_id, pwbl.domain_id, pwbl.email, pwbl.action FROM pop3_wblist pwbl WHERE pwbl.domain_id = :domain_id";
        sqlParam = new MapSqlParameterSource("domain_id", domainId);

        List<String> writeList = new ArrayList<>();
        List<String> blackList = new ArrayList<>();

        rowSet = jdbcTemplate.queryForRowSet(query, sqlParam);
        while (rowSet.next()) {
            String actionStr = rowSet.getString("action");
            String email = rowSet.getString("email");

            if (!isValidEmailOrDomain(email)) {
                continue;
            }
            switch (actionStr) {
                case "WHITE":
                    writeList.add(email);
                    break;
                case "BLACK":
                    blackList.add(email);
                    break;
            }
        }

        mailbox.setWhiteList(writeList);
        mailbox.setBlackList(blackList);

        governorOfMailbox.validateAndStore(mailbox);

        return null;
    }

    public boolean importToMongo(String accountId, boolean switchedOn, boolean allowAntispam) {
        List<Mailbox> mailboxes = mailboxRepository.findByAccountId(accountId);

        if (mailboxes != null && !mailboxes.isEmpty()) {
            mailboxRepository.deleteAll(mailboxes);
        }

        pull(accountId, switchedOn, allowAntispam);
        return true;
    }


}
