package ru.majordomo.hms.rc.user.event.mailbox.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.mailbox.MailboxCreateEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxImportEvent;
import ru.majordomo.hms.rc.user.importing.MailboxDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.Mailbox;

@Component
public class MailboxEventListener {
    private final static Logger logger = LoggerFactory.getLogger(MailboxEventListener.class);

    private final GovernorOfMailbox governorOfMailbox;
    private final MailboxDBImportService mailboxDBImportService;

    @Autowired
    public MailboxEventListener(
            GovernorOfMailbox governorOfMailbox,
            MailboxDBImportService mailboxDBImportService) {
        this.governorOfMailbox = governorOfMailbox;
        this.mailboxDBImportService = mailboxDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onMailboxCreateEvent(MailboxCreateEvent event) {
        Mailbox mailbox = event.getSource();

        logger.debug("We got MailboxCreateEvent");

        try {
            governorOfMailbox.validateAndStore(mailbox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onMailboxImportEvent(MailboxImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got MailboxImportEvent");

        try {
            mailboxDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
