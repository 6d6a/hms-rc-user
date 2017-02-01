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
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;

@EnableRabbit
@Service
public class DatabaseAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "database.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("database", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("database", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database.update",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "database.update", type = "topic"),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("database", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("database", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "database.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("database", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("database", serviceMessage);
                break;
        }
    }
}
