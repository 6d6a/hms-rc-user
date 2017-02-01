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
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;

@EnableRabbit
@Service
public class UnixAccountAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfUnixAccount governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.unix-account.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "unix-account.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("unix-account", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("unix-account", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.unix-account.update",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "unix-account.update", type = "topic"),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("unix-account", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("unix-account", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.unix-account.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "unix-account.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("unix-account", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("unix-account", serviceMessage);
                break;
        }
    }
}
