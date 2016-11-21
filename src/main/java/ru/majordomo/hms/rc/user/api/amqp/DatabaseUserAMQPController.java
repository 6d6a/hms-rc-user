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
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@Service
public class DatabaseUserAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfDatabaseUser governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database-user.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "database-user.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("database-user", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("database-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database-user.delete",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "database-user.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("database-user", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("database-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database-user.update",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "database-user.update", type = "topic"),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("database-user", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("database-user", serviceMessage);
                break;
        }
    }

}
