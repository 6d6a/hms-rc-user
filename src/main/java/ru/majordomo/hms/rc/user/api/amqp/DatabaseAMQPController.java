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

    private GovernorOfDatabase governor;

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.database.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "database.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("database", serviceMessage, governor);
                break;
            case ("te"):
                handleCreateEventFromTE("database", serviceMessage, governor);
                break;
        }
    }
}
