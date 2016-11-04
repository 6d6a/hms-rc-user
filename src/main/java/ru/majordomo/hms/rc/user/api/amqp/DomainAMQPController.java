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
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;

@EnableRabbit
@Service
public class DomainAMQPController extends BaseAMQPController {

    private GovernorOfDomain governor;

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.domain.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "domain.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("domain", serviceMessage, governor);
                break;
            case ("te"):
                handleCreateEventFromTE("domain", serviceMessage, governor);
                break;
        }
    }
}
