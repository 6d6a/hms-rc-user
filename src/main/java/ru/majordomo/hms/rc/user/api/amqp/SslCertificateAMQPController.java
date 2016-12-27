package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificateState;

@EnableRabbit
@Service
public class SslCertificateAMQPController extends BaseAMQPController {

    @Autowired
    private Sender sender;

    @Autowired
    public void setGovernor(GovernorOfSSLCertificate governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ssl-certificate.create",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "ssl-certificate.create", type = "topic"),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                governor.create(serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("ssl-certificate", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ssl-certificate.delete",
            durable = "true", autoDelete = "true"),
            exchange = @Exchange(value = "ssl-certificate.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                String resourceId = (String) serviceMessage.getParam("resourceId");
                governor.drop(resourceId);
                break;
            case ("te"):
                handleDeleteEventFromTE("ssl-certificate", serviceMessage);
                break;
        }
    }
}
