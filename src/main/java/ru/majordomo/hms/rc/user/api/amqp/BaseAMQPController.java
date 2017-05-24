package ru.majordomo.hms.rc.user.api.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

@Component
@EnableRabbit
class BaseAMQPController {

    protected String applicationName;
    private Sender sender;
    private StaffResourceControllerClient staffRcClient;
    private static final Logger logger = LoggerFactory.getLogger(BaseAMQPController.class);

    protected LordOfResources governor;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Autowired
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    private Resource getResourceByUrl(String url) {
        Resource resource = null;
        try {
            URL processingUrl = new URL(url);
            String path = processingUrl.getPath();
            String[] pathParts = path.split("/");
            String resourceId = pathParts[2];
            resource = governor.build(resourceId);
        } catch (MalformedURLException e) {
            logger.warn("Ошибка при обработке URL:" + url);
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found");
        }

        return resource;
    }

    void handleCreateEventFromPM(String resourceType,
                                 ServiceMessage serviceMessage) {

        Boolean success;
        Resource resource = null;
        String errorMessage = "";

        try {
            resource = governor.create(serviceMessage);
            success = true;
        } catch (ConstraintViolationException e) {
            errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Создание ресурса " + resourceType + " не удалось: " + errorMessage);
            success = false;
        } catch (Exception e) {
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Создание ресурса " + resourceType + " не удалось: " + e.getMessage());
            errorMessage = e.getMessage();
            success = false;
        }

        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
        report.addParam("success", success);

        if (success && (resource instanceof ServerStorable || resource instanceof Serviceable)) {
            try {
                String teRoutingKey = getTaskExecutorRoutingKey(resource);
                sender.send(resourceType + ".create", teRoutingKey, report);
            } catch (ParameterValidateException e) {
                errorMessage = e.getMessage();
                serviceMessage.delParam("success");
                serviceMessage.addParam("success", false);
                report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
                sender.send(resourceType + ".create", "pm", report);
            }
        } else {
            sender.send(resourceType + ".create", "pm", report);
        }
    }

    void handleCreateEventFromTE(String resourceType,
                                 ServiceMessage serviceMessage) {

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        Resource resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);

        if (!successEvent && resource != null) {
            governor.drop(resource.getId());
        }

        sender.send(resourceType + ".create", "pm", report);
    }

    void handleUpdateEventFromPM(String resourceType,
                                 ServiceMessage serviceMessage) {

        Boolean success;
        Resource resource = null;
        String errorMessage = "";

        try {
            resource = governor.update(serviceMessage);
            success = true;
        } catch (ConstraintViolationException e) {
            errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Обновление ресурса " + resourceType + " не удалось: " + errorMessage);
            success = false;
        } catch (Exception e) {
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Обновление ресурса " + resourceType + " не удалось: " + e.getMessage());
            errorMessage = e.getMessage();
            success = false;
        }

        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
        report.addParam("success", success);

        if (success && (resource instanceof ServerStorable || resource instanceof Serviceable) && !resourceType.equals("mailbox")) {
            String teRoutingKey = getTaskExecutorRoutingKey(resource);
            sender.send(resourceType + ".update", teRoutingKey, report);
        } else {
            sender.send(resourceType + ".update", "pm", report);
        }
    }

    void handleUpdateEventFromTE(String resourceType,
                                 ServiceMessage serviceMessage) {

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        Resource resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
        report.addParam("success", successEvent);

        sender.send(resourceType + ".update", "pm", report);
    }

    void handleDeleteEventFromPM(String resourceType, ServiceMessage serviceMessage) {

        String errorMessage = "";
        String resourceId = null;
        Resource resource = null;

        String accountId = serviceMessage.getAccountId();

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = serviceMessage.getParam("resourceId").toString();
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        keyValue.put("resourceId", resourceId);

        try {
            resource = governor.build(keyValue);
        } catch (ResourceNotFoundException e) {
            errorMessage = "Ресурс " + resourceType + " с ID: " + resourceId + " и accountId: " + accountId + " не найден";
            ServiceMessage report = createReportMessage(serviceMessage, resourceType, null, errorMessage);
            report.addParam("success", false);
            sender.send(resourceType + ".delete", "pm", report);
        }

        if (resource != null) {
            ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
            report.addParam("success", true);
            if (resource instanceof ServerStorable || resource instanceof Serviceable) {
                String teRoutingKey = getTaskExecutorRoutingKey(resource);
                try {
                    governor.preDelete(resourceId);
                } catch (ParameterValidateException e) {
                    report.delParam("success");
                    report.delParam("errorMessage");
                    report.addParam("success", false);
                    report.addParam("errorMessage", e.getMessage());
                }
                sender.send(resourceType + ".delete", teRoutingKey, report);
            } else {
                try {
                    governor.drop(resourceId);
                } catch (ParameterValidateException e) {
                    report.delParam("success");
                    report.delParam("errorMessage");
                    report.addParam("success", false);
                    report.addParam("errorMessage", e.getMessage());
                }
                sender.send(resourceType + ".delete", "pm", report);
            }
        }
    }

    void handleDeleteEventFromTE(String resourceType,
                                             ServiceMessage serviceMessage) {

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        Resource resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);

        if (successEvent && resource != null) {
            governor.drop(resource.getId());
        }

        sender.send(resourceType + ".delete", "pm", report);

    }

    private ServiceMessage createReportMessage(ServiceMessage event,
                                               String resourceType,
                                               Resource resource, String errorMessage) {
        ServiceMessage report = new ServiceMessage();
        report.setActionIdentity(event.getActionIdentity());
        report.setOperationIdentity(event.getOperationIdentity());
        report.setAccountId(event.getAccountId());
        if (resource != null) {
            report.setObjRef("http://" + applicationName + "/" + resourceType + "/" + resource.getId());
        }
        Boolean eventSuccess = (Boolean) event.getParam("success");
        if (eventSuccess == null) {
            report.addParam("success", true);
        } else {
            report.addParam("success", eventSuccess);
        }

        report.addParam("errorMessage", errorMessage);

        return report;
    }

    private String getTaskExecutorRoutingKey(Resource resource) throws ParameterValidateException {
        try {
            String serverName = null;
            if (resource instanceof ServerStorable) {
                ServerStorable serverStorable = (ServerStorable) resource;
                serverName = staffRcClient.getServerById(serverStorable.getServerId()).getName();
            } else if (resource instanceof Serviceable) {
                Serviceable serviceable = (Serviceable) resource;
                serverName = staffRcClient.getServerByServiceId(serviceable.getServiceId()).getName();
            }

            return "te" + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            throw new ParameterValidateException("Exception: " + e.getMessage());
        }
    }

}
