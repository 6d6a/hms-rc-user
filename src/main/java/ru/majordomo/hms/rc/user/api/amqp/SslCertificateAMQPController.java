package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.repositories.SslCertificateActionIdentityRepository;
import ru.majordomo.hms.rc.user.resources.DTO.SslCertificateActionIdentity;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificateState;
import ru.majordomo.hms.rc.user.resources.WebSite;

@EnableRabbit
@Service
public class SslCertificateAMQPController extends BaseAMQPController {

    @Autowired
    private Sender sender;

    private SslCertificateActionIdentityRepository actionIdentityRepository;

    @Autowired
    public void setActionIdentityRepository(SslCertificateActionIdentityRepository actionIdentityRepository) {
        this.actionIdentityRepository = actionIdentityRepository;
    }

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
                SSLCertificate sslCertificate = (SSLCertificate) governor.create(serviceMessage);
                sendReportToTE(sslCertificate.getId());
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

    private void sendReportToTE(String resourceId) {
        ServiceMessage report = ((GovernorOfSSLCertificate) governor).createSslCertificateServiceMessageForTE(resourceId);
        sender.send("ssl-certificate.delete", ((GovernorOfSSLCertificate) governor).getTaskExecutorRoutingKeyForSslCertificate(resourceId), report);
    }
}
