package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
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

@Service
public class MailboxAMQPController extends BaseAMQPController<Mailbox> {

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return "mailbox";
    }
}
