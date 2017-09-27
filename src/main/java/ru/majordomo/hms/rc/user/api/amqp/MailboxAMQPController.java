package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.Mailbox;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.MAILBOX_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.MAILBOX_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.MAILBOX_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class MailboxAMQPController extends BaseAMQPController<Mailbox> {

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + MAILBOX_CREATE)
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("mailbox", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("mailbox", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + MAILBOX_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("mailbox", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("mailbox", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + MAILBOX_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("mailbox", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("mailbox", serviceMessage);
                break;
        }
    }
}
