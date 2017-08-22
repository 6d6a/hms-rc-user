package ru.majordomo.hms.rc.user.importing;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.majordomo.hms.rc.user.event.mailbox.MailboxCommentImportEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxImportEvent;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.resources.*;

@Service
public class MailboxCommentsDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(MailboxCommentsDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final MailboxRepository mailboxRepository;
    private final GovernorOfMailbox governorOfMailbox;
    private final DomainRepository domainRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public MailboxCommentsDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            MailboxRepository mailboxRepository,
            GovernorOfMailbox governorOfMailbox,
            DomainRepository domainRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.mailboxRepository = mailboxRepository;
        this.governorOfMailbox = governorOfMailbox;
        this.domainRepository = domainRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM POP3 p " +
                "JOIN domain d ON d.Domain_ID = p.Domain_ID " +
                "JOIN account a ON d.acc_id = a.id " +
                "JOIN POP3_comments c ON c.pop3_id=p.id " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            logger.info("[start] found MC for acc " + resultSet.getString("id"));
            publisher.publishEvent(new MailboxCommentImportEvent(resultSet.getString("id")));
            logger.info("[stop] found MC for acc " + resultSet.getString("id"));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, " +
                "p.username, " +
                "d.Domain_name, " +
                "c.comment " +
                "FROM POP3 p " +
                "JOIN domain d ON d.Domain_ID = p.Domain_ID " +
                "JOIN account a ON d.acc_id = a.id " +
                "JOIN POP3_comments c ON c.pop3_id=p.id " +
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

        Domain domain = domainRepository.findByNameAndAccountId(
                domainName,
                rs.getString("id")
        );

        if (domain != null) {
            Mailbox mailbox = mailboxRepository.findByNameAndDomainId(name, domain.getId());

            if (mailbox != null) {
                String comment = null;
                try {
                    comment = EncodingUtils.getString(rs.getString("comment").getBytes("windows-1251"), "koi8-r");

                    if (comment.trim().equals("")) {
                        return null;
                    }

                    mailbox.setComment(comment);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    logger.error("Exception in accountHistory.setMessage: " + e.getMessage());
                }

                try {
                    governorOfMailbox.preValidate(mailbox);

                    governorOfMailbox.validateImported(mailbox);

                    governorOfMailbox.store(mailbox);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                logger.info("mailbox " + mailbox + " comment saved " + comment);
            } else {
                logger.error("not found mailbox: " + name + "@" + domainName);
            }
        } else {
            logger.error("not found domain for mailbox: " + name + "@" + domainName);
            return null;
        }

        return null;
    }

    public boolean importToMongo() {
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        pull(accountId);
        return true;
    }
}
