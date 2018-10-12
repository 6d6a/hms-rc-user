package ru.majordomo.hms.rc.user.event.mailbox.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxCommentImportEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxCreateEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxImportEvent;
import ru.majordomo.hms.rc.user.importing.MailboxCommentsDBImportService;
import ru.majordomo.hms.rc.user.importing.MailboxDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.Mailbox;

@Component
@Profile("import")
public class MailboxEventListener extends ResourceEventListener<Mailbox> {
    private final MailboxCommentsDBImportService mailboxCommentsDBImportService;

    @Autowired
    public MailboxEventListener(
            GovernorOfMailbox governorOfMailbox,
            MailboxDBImportService mailboxDBImportService,
            MailboxCommentsDBImportService mailboxCommentsDBImportService
    ) {
        this.mailboxCommentsDBImportService = mailboxCommentsDBImportService;
        this.governor = governorOfMailbox;
        this.dbImportService = mailboxDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(MailboxCreateEvent event) {
        logger.debug("We got CreateEvent");

        Mailbox mailbox = event.getSource();
        try {
            governor.preValidate(mailbox);
            governor.syncWithRedis(mailbox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(MailboxImportEvent event) {
        processImportEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCommentImportEvent(MailboxCommentImportEvent event) {
        mailboxCommentsDBImportService.pull(event.getSource());
    }
}
