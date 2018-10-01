package ru.majordomo.hms.rc.user.api.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class SslCertificateAMQPController {

    private static final Logger logger = LoggerFactory.getLogger(SslCertificateAMQPController.class);

    private String applicationName;
    private String instanceName;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Value("${hms.instance.name}")
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    private GovernorOfSSLCertificate governor;

    private Sender sender;

    @Autowired
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @Autowired
    public void setGovernor(GovernorOfSSLCertificate governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + SSL_CERTIFICATE_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateSslEventFromPM(serviceMessage);
                break;
            case LETSENCRYPT:
                handleCreateSslEventFromLetsEncrypt(serviceMessage);
                break;
            case TE:
                handleCreateSslEventFromTE(serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + SSL_CERTIFICATE_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteSslEventFromPM(serviceMessage);
                break;
            case TE:
                handleDeleteSslEventFromTE(serviceMessage);
                break;
        }
    }

    private void handleCreateSslEventFromPM(ServiceMessage serviceMessage) {
        if (serviceMessage.getParam("name") == null || serviceMessage.getParam("name").equals("")) {
            ServiceMessage report = createReport(serviceMessage, null, "Необходимо указать имя домена в поле name");
            report.addParam("success", false);
            sender.send(SSL_CERTIFICATE_CREATE, PM, report);
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

                if (certificate.getNotAfter().isBefore(LocalDateTime.now())) {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(certificate);
                    serviceMessage.addParam("sslCertificate", json);

                    sender.send(SSL_CERTIFICATE_UPDATE, LETSENCRYPT, serviceMessage);
                    return;
                }

                governor.validate(certificate);
                governor.store(certificate);

                ServiceMessage report = createReport(serviceMessage, certificate, "");
                String teRoutingKey = governor.getTERoutingKey(certificate.getId());

                if (teRoutingKey != null) {
                    sender.send(SSL_CERTIFICATE_CREATE, teRoutingKey, report);
                } else {
                    sender.send(SSL_CERTIFICATE_CREATE, PM, report);
                }
            } else {
                sender.send(SSL_CERTIFICATE_CREATE, LETSENCRYPT, serviceMessage);
            }
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Создание ресурса SSLCertificate не удалось: " + errorMessage);
            ServiceMessage report = createReport(serviceMessage, null, errorMessage);
            report.delParam("success");
            report.addParam("success", false);
            sender.send(SSL_CERTIFICATE_CREATE, PM, report);
        } catch (Exception e) {
            ServiceMessage report = createReport(serviceMessage, null, e.getMessage());
            report.delParam("success");
            report.addParam("success", false);
            sender.send(SSL_CERTIFICATE_CREATE, PM, report);
        }
    }

    private void handleCreateSslEventFromLetsEncrypt(ServiceMessage serviceMessage) {
        Boolean success = (Boolean) serviceMessage.getParam("success");
        ServiceMessage report;
        SSLCertificate certificate = null;
        if (success) {
            try {
                certificate = governor.create(serviceMessage);
                String teRoutingKey = governor.getTERoutingKey(certificate.getId());

                report = createReport(serviceMessage, certificate, (String) serviceMessage.getParam("errorMessage"));
                if (teRoutingKey != null) {
                    sender.send(SSL_CERTIFICATE_CREATE, teRoutingKey, report);
                } else {
                    sender.send(SSL_CERTIFICATE_CREATE, PM, report);
                }
            } catch (ConstraintViolationException e) {
                String errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
                logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                        " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                        " Создание ресурса SSLCertificate не удалось: " + errorMessage);
                report = createReport(serviceMessage, null, errorMessage);
                report.delParam("success");
                report.addParam("success", false);
                sender.send(SSL_CERTIFICATE_CREATE, PM, report);
            } catch (Exception e) {
                logger.error(e.getMessage());
                report = createReport(serviceMessage, null, e.getMessage());
                report.delParam("success");
                report.addParam("success", false);
                sender.send(SSL_CERTIFICATE_CREATE, PM, report);
            }
        } else {
            String resourceId = (String) serviceMessage.getParam("resourceId");

            if (resourceId != null) {
                try {
                    certificate = governor.build(resourceId);
                    LocalDateTime notAfter = governor.getNotAfterFromCert(certificate);
                    if (notAfter.isBefore(LocalDateTime.now())) {
                        governor.realDrop(resourceId);
                    }
                } catch (Exception ignore) {}
            }

            report = createReport(serviceMessage, null, "");
            sender.send(SSL_CERTIFICATE_CREATE, PM, report);
        }
    }

    private void handleCreateSslEventFromTE(ServiceMessage serviceMessage) {
        sender.send(SSL_CERTIFICATE_CREATE, PM, serviceMessage);
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
            sender.send(SSL_CERTIFICATE_DELETE, PM, report);
        } catch (Exception e) {
            ServiceMessage report = createReport(serviceMessage, null, e.getMessage());
            sender.send(SSL_CERTIFICATE_DELETE, PM, report);
        }
        String teRoutingKey = governor.getTERoutingKey(certificate.getId());

        ServiceMessage report = createReport(serviceMessage, certificate, (String) serviceMessage.getParam("errorMessage"));
        if (teRoutingKey != null) {
            sender.send(SSL_CERTIFICATE_DELETE, teRoutingKey, report);
        } else {
            sender.send(SSL_CERTIFICATE_DELETE, PM, report);
        }
    }

    private void handleDeleteSslEventFromTE(ServiceMessage serviceMessage) {
        sender.send(SSL_CERTIFICATE_DELETE, PM, serviceMessage);
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
        
        if (serviceMessage.getParams().containsKey("isSafeBrowsing")) {
            report.addParam("isSafeBrowsing", serviceMessage.getParam("isSafeBrowsing"));
        }

        return report;
    }

    private String getRealProviderName(String eventProvider) {
        return eventProvider.replaceAll("^" + instanceName + "\\.", "");
    }
}
