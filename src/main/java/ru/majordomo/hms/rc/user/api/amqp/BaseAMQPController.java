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

import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Component
@EnableRabbit
abstract class BaseAMQPController<T extends Resource> {

    protected String applicationName;
    protected String instanceName;

    private Sender sender;
    private StaffResourceControllerClient staffRcClient;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected LordOfResources<T> governor;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Value("${hms.instance_name}")
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Autowired
    public void setSender(Sender sender) {
        this.sender = sender;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    private T getResourceByUrl(String url) {
        T resource = null;
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

    void handleCreateEventFromPM(
            String resourceType,
            ServiceMessage serviceMessage
    ) {

        Boolean success;
        T resource = null;
        String errorMessage = "";

        try {
            resource = governor.create(serviceMessage);
            success = true;
        } catch (ConstraintViolationException e) {
            errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(". "));
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
                serviceMessage.addParam("success", false);
                report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
                sender.send(resourceType + ".create", PM, report);
            }
            resource.setLocked(true);
            governor.store(resource);
        } else {
            sender.send(resourceType + ".create", PM, report);
        }
    }

    void handleCreateEventFromTE(
            String resourceType,
            ServiceMessage serviceMessage
    ) {
        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        T resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);

        if (resource != null) {
            if (!successEvent) {
                governor.drop(resource.getId());
            } else {
                resource.setLocked(false);
                governor.store(resource);
            }
        }

        sender.send(resourceType + ".create", PM, report);
    }

    void handleUpdateEventFromPM(
            String resourceType,
            ServiceMessage serviceMessage
    ) {
        Boolean success;
        T resource = null;
        String errorMessage = "";

        try {
            String resourceId = (String) serviceMessage.getParam("resourceId");
            if (resourceId == null || resourceId.equals("")) {
                throw new ParameterValidateException("Не указан resourceId");
            }
            try {
                resource = governor.build(resourceId);
            } catch (Exception e) {
                throw new ParameterValidateException("Не найден ресурс с ID: " + resourceId);
            }
            if (resource.isLocked()) {
                throw new ParameterValidateException("Ресурс в процессе обновления");
            }
        } catch (ParameterValidateException e) {
            logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                    " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                    " Обновление ресурса " + resourceType + " не удалось: " + e.getMessage());
            errorMessage = e.getMessage();

            ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
            report.addParam("success", false);
            sender.send(resourceType + ".update", PM, report);
            return;
        }

        try {
            resource = governor.update(serviceMessage);
            success = true;
        } catch (ConstraintViolationException e) {
            errorMessage = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(". "));
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
            resource.setLocked(true);
            governor.store(resource);
        } else {
            sender.send(resourceType + ".update", PM, report);
        }
    }

    void handleUpdateEventFromTE(
            String resourceType,
            ServiceMessage serviceMessage
    ) {
        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        T resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
        report.addParam("success", successEvent);

        if (resource != null) {
            resource.setLocked(false);
            governor.store(resource);
        }

        sender.send(resourceType + ".update", PM, report);
    }

    void handleDeleteEventFromPM(
            String resourceType,
            ServiceMessage serviceMessage
    ) {
        String errorMessage = "";
        String resourceId = null;
        T resource = null;

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
            sender.send(resourceType + ".delete", PM, report);
        } catch (ParameterValidateException e) {
            errorMessage = "Обработка ресурса " + resourceType + " с ID: " + resourceId + " и accountId: " + accountId + " не удалась";
            ServiceMessage report = createReportMessage(serviceMessage, resourceType, null, errorMessage);
            report.addParam("success", false);
            sender.send(resourceType + ".delete", PM, report);
        }

        if (resource != null) {
            if (resource.isLocked()) {
                logger.error("ACTION_IDENTITY: " + serviceMessage.getActionIdentity() +
                        " OPERATION_IDENTITY: " + serviceMessage.getOperationIdentity() +
                        " Обновление ресурса " + resourceType + " не удалось: locked");
                errorMessage = "Ресурс в процессе обновления";
                ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
                report.addParam("success", false);
                sender.send(resourceType + ".update", PM, report);
                return;
            }
            ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);
            report.addParam("success", true);
            if (resource instanceof ServerStorable || resource instanceof Serviceable) {
                String teRoutingKey = getTaskExecutorRoutingKey(resource);
                try {
                    governor.preDelete(resourceId);
                } catch (ParameterValidateException e) {
                    report.addParam("success", false);
                    report.addParam("errorMessage", e.getMessage());
                }
                sender.send(resourceType + ".delete", teRoutingKey, report);
                resource.setLocked(true);
                governor.store(resource);
            } else {
                try {
                    governor.drop(resourceId);
                } catch (ParameterValidateException e) {
                    report.addParam("success", false);
                    report.addParam("errorMessage", e.getMessage());
                }
                sender.send(resourceType + ".delete", PM, report);
            }
        }
    }

    void handleDeleteEventFromTE(
            String resourceType,
            ServiceMessage serviceMessage
    ) {
        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        T resource = getResourceByUrl(resourceUrl);
        String errorMessage = (String) serviceMessage.getParam("errorMessage");
        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource, errorMessage);

        if (resource != null) {
            if (successEvent){
                governor.drop(resource.getId());
            } else {
                resource.setLocked(false);
                governor.store(resource);
            }
        }

        sender.send(resourceType + ".delete", PM, report);

    }

    private ServiceMessage createReportMessage(
            ServiceMessage event,
            String resourceType,
            T resource,
            String errorMessage
    ) {
        ServiceMessage report = new ServiceMessage();
        report.setActionIdentity(event.getActionIdentity());
        report.setOperationIdentity(event.getOperationIdentity());
        report.setAccountId(event.getAccountId());
        if (resource != null) {
            report.setObjRef("http://" + applicationName + "/" + resourceType + "/" + resource.getId());
            report.addParam("name", resource.getName());

            if (report.getAccountId() == null || report.getAccountId().equals("")) {
                report.setAccountId(resource.getAccountId());
            }
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

    private String getTaskExecutorRoutingKey(T resource) throws ParameterValidateException {
        try {
            String serverName = null;
            if (resource instanceof ServerStorable) {
                ServerStorable serverStorable = (ServerStorable) resource;
                serverName = staffRcClient.getServerById(serverStorable.getServerId()).getName();
            } else if (resource instanceof Serviceable) {
                Serviceable serviceable = (Serviceable) resource;
                serverName = staffRcClient.getServerByServiceId(serviceable.getServiceId()).getName();
            }

            return TE + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[getTaskExecutorRoutingKey] got exception: " + e.getMessage());
            throw new ParameterValidateException("Exception: " + e.getMessage());
        }
    }

    protected String getRealProviderName(String eventProvider) {
        return eventProvider.replaceAll("^" + instanceName + "\\.", "");
    }
}
