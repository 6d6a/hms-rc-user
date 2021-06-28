package ru.majordomo.hms.rc.user.event.mailbox;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.event.ResourceIdEvent;
import ru.majordomo.hms.rc.user.resources.DTO.EntityIdOnly;
import ru.majordomo.hms.rc.user.resources.Mailbox;

import javax.annotation.ParametersAreNonnullByDefault;

public class MailboxRedisEvent extends ResourceIdEvent {
    public MailboxRedisEvent(String mailboxId) {
        super(mailboxId);
    }

    public MailboxRedisEvent(EntityIdOnly mailboxIdOnly) {
        super(mailboxIdOnly.getId());
    }
}
