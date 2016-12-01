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
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;

@EnableRabbit
@Service
public class FTPUserAMQPController extends BaseAMQPController {

    @Autowired
    public void setGovernor(GovernorOfFTPUser governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ftp-user.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "ftp-user.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("ftp-user", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("ftp-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ftp-user.update",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "ftp-user.update", type = "topic"),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("ftp-user", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("ftp-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ftp-user.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "ftp-user.create", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("ftp-user", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("ftp-user", serviceMessage);
                break;
        }
    }

}
