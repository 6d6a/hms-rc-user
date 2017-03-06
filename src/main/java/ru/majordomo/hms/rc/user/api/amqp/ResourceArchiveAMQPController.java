package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;

@EnableRabbit
@Service
public class ResourceArchiveAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfResourceArchive governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.resource-archive.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "resource-archive.create", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("resource-archive", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("resource-archive", serviceMessage);
                break;
        }
    }

//    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.resource-archive.update",
//            durable = "true", autoDelete = "false"),
//            exchange = @Exchange(value = "resource-archive.update", type = ExchangeTypes.TOPIC),
//            key = "rc.user"))
//    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
//                                  @Payload ServiceMessage serviceMessage) {
//        switch (eventProvider) {
//            case ("pm"):
//                handleUpdateEventFromPM("resource-archive", serviceMessage);
//                break;
//            case ("te"):
//                handleUpdateEventFromTE("resource-archive", serviceMessage);
//                break;
//        }
//    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.resource-archive.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "resource-archive.delete", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("resource-archive", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("resource-archive", serviceMessage);
                break;
        }
    }
}
