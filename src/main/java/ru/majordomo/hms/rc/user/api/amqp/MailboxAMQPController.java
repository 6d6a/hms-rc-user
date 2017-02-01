package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;

@EnableRabbit
@Service
public class MailboxAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.mailbox.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "mailbox.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("mailbox", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("mailbox", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.mailbox.update",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "mailbox.update", type = "topic"),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("mailbox", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("mailbox", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.mailbox.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "mailbox.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("mailbox", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("mailbox", serviceMessage);
                break;
        }
    }
}
