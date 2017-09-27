package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.ExchangeTypes;
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
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

@EnableRabbit
@Service
public class PersonAMQPController extends BaseAMQPController<Person> {

    @Autowired
    public void setGovernor(GovernorOfPerson governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.person.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "person.create", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("person", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("person", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.person.update",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "person.update", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("person", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("person", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.person.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "person.delete", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("person", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("person", serviceMessage);
                break;
        }
    }
}
