package ru.majordomo.hms.rc.user.api.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@EnableRabbit
@Service
public class SslCertificateAMQPController {

    private static final Logger logger = LoggerFactory.getLogger(SslCertificateAMQPController.class);

    private String applicationName;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    private GovernorOfSSLCertificate governor;

    @Autowired
    private Sender sender;

    @Autowired
    public void setGovernor(GovernorOfSSLCertificate governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ssl-certificate.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "ssl-certificate.create", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateSslEventFromPM(serviceMessage);
                break;
            case ("letsencrypt"):
                handleCreateSslEventFromLetsEncrypt(serviceMessage);
                break;
            case ("te"):
                handleCreateSslEventFromTE(serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.ssl-certificate.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "ssl-certificate.delete", type = "topic"),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider", required = false) String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteSslEventFromPM(serviceMessage);
                break;
            case ("te"):
                handleDeleteSslEventFromTE(serviceMessage);
                break;
        }
    }

    private void handleCreateSslEventFromPM(ServiceMessage serviceMessage) {
        if (serviceMessage.getParam("name") == null || serviceMessage.getParam("name").equals("")) {
            ServiceMessage report = createReport(serviceMessage, null, "Необходимо указать имя домена в поле name");
            report.addParam("success", false);
            sender.send("ssl-certificate.create", "pm", report);
            return;
        }
        try {
            String name = (String) serviceMessage.getParam("name");
//            String accountId = serviceMessage.getAccountId();
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("name", name);
            //TODO подумать насколько это плохо ->
            //TODO -> (если есть cert на другом акке для этого домена, то забирать его на новый акк)
//            keyValue.put("accountId", accountId);

            if (governor.exists(keyValue)) {
                SSLCertificate certificate = governor.update(serviceMessage);

                if (certificate.getNotAfter().isBefore(LocalDateTime.now().plusDays(5))) {
                    sender.send("ssl-certificate.create", "letsencrypt", serviceMessage, "rc");
                    return;
                }

                governor.validate(certificate);
                governor.store(certificate);

                ServiceMessage report = createReport(serviceMessage, certificate, "");
                String teRoutingKey = governor.getTERoutingKey(certificate.getId());

                if (teRoutingKey != null) {
                    sender.send("ssl-certificate.create", teRoutingKey, report);
                } else {
                    sender.send("ssl-certificate.create", "pm", report);
                }
            } else {
                sender.send("ssl-certificate.create", "letsencrypt", serviceMessage, "rc");
            }
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Создание ресурса SSLCertificate не удалось: " + errorMessage);
            ServiceMessage report = createReport(serviceMessage, null, errorMessage);
            report.delParam("success");
            report.addParam("success", false);
            sender.send("ssl-certificate.create", "pm", report);
        } catch (Exception e) {
            ServiceMessage report = createReport(serviceMessage, null, e.getMessage());
            report.delParam("success");
            report.addParam("success", false);
            sender.send("ssl-certificate.create", "pm", report);
        }
    }

    private void handleCreateSslEventFromLetsEncrypt(ServiceMessage serviceMessage) {
        Boolean success = (Boolean) serviceMessage.getParam("success");
        ServiceMessage report;
        if (success) {
            try {
                SSLCertificate certificate = governor.create(serviceMessage);
                String teRoutingKey = governor.getTERoutingKey(certificate.getId());

                report = createReport(serviceMessage, certificate, (String) serviceMessage.getParam("errorMessage"));
                if (teRoutingKey != null) {
                    sender.send("ssl-certificate.create", teRoutingKey, report);
                } else {
                    sender.send("ssl-certificate.create", "pm", report);
                }
            } catch (ConstraintViolationException e) {
                String errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
                logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                        " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                        " Создание ресурса SSLCertificate не удалось: " + errorMessage);
                report = createReport(serviceMessage, null, errorMessage);
                report.delParam("success");
                report.addParam("success", false);
                sender.send("ssl-certificate.create", "pm", report);
            } catch (Exception e) {
                logger.error(e.getMessage());
                report = createReport(serviceMessage, null, e.getMessage());
                report.delParam("success");
                report.addParam("success", false);
                sender.send("ssl-certificate.create", "pm", report);
            }
        } else {
            report = createReport(serviceMessage, null, "");
            sender.send("ssl-certificate.create", "pm", report);
        }
    }

    private void handleCreateSslEventFromTE(ServiceMessage serviceMessage) {
        sender.send("ssl-certificate.create", "pm", serviceMessage);
    }

    private void handleDeleteSslEventFromPM(ServiceMessage serviceMessage) {
        String resourceId = (String) serviceMessage.getParam("resourceId");
        SSLCertificate certificate = new SSLCertificate();
        try {
            governor.drop(resourceId);
            certificate = governor.build(resourceId);
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Удаление ресурса SSLCertificate не удалось: " + errorMessage);
            ServiceMessage report = createReport(serviceMessage, null, errorMessage);
            report.delParam("success");
            report.addParam("success", false);
            sender.send("ssl-certificate.create", "pm", report);
        } catch (Exception e) {
            ServiceMessage report = createReport(serviceMessage, null, e.getMessage());
            sender.send("ssl-certificate.delete", "pm", report);
        }
        String teRoutingKey = governor.getTERoutingKey(certificate.getId());

        ServiceMessage report = createReport(serviceMessage, certificate, (String) serviceMessage.getParam("errorMessage"));
        if (teRoutingKey != null) {
            sender.send("ssl-certificate.delete", teRoutingKey, report);
        } else {
            sender.send("ssl-certificate.delete", "pm", report);
        }
    }

    private void handleDeleteSslEventFromTE(ServiceMessage serviceMessage) {
        sender.send("ssl-certificate.delete", "pm", serviceMessage);
    }

    private ServiceMessage createReport(ServiceMessage serviceMessage, SSLCertificate sslCertificate, String errorMessage) {

        ServiceMessage report = new ServiceMessage();
        report.setActionIdentity(serviceMessage.getActionIdentity());
        report.setOperationIdentity(serviceMessage.getOperationIdentity());
        report.setAccountId(serviceMessage.getAccountId());
        if (sslCertificate != null) {
            report.setObjRef("http://" + applicationName + "/ssl-certificate/" + sslCertificate.getId());
        }
        Boolean eventSuccess = (Boolean) serviceMessage.getParam("success");
        if (eventSuccess == null) {
            report.addParam("success", true);
        } else {
            report.addParam("success", eventSuccess);
        }

        report.addParam("errorMessage", errorMessage);

        return report;
    }
}
