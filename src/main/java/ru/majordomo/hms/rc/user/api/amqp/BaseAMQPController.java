package ru.majordomo.hms.rc.user.api.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;

@Component
@EnableRabbit
class BaseAMQPController {

    private String applicationName;
    private Sender sender;
    private StaffResourceControllerClient staffRcClient;
    private static final Logger logger = LoggerFactory.getLogger(BaseAMQPController.class);

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

    private Resource getResourceByUrl(String url, LordOfResources governor) {
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
        }

        return resource;
    }

    void handleCreateEventFromPM(String resourceType,
                                 ServiceMessage serviceMessage,
                                 LordOfResources governor) {
        List<String> serverStorableResourceTypes = Arrays.asList(
                "database",
                "website",
                "mailbox",
                "unix-account",
                "ftp-user",
                "database-user");

        Boolean success;
        Resource resource = null;

        try {
            resource = governor.create(serviceMessage);
            success = true;
        } catch (ParameterValidateException e) {
            logger.error("Создание ресурса не удалось:" + e.getMessage());
            success = false;
        }

        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource);
        report.addParam("success", success);

        if (success && serverStorableResourceTypes.contains(resourceType)) {
            String teRoutingKey = getTaskExecutorRoutingKey(resource);
            sender.send(resourceType + ".create", teRoutingKey, report);
        } else {
            sender.send(resourceType + ".create", "pm", report);
        }
    }

    void handleCreateEventFromTE(String resourceType,
                                 ServiceMessage serviceMessage,
                                 LordOfResources governor) {

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String resourceUrl = serviceMessage.getObjRef();
        Resource resource = getResourceByUrl(resourceUrl, governor);

        ServiceMessage report = createReportMessage(serviceMessage, resourceType, resource);

        if (!successEvent) {
            governor.drop(resource.getId());
        }

        sender.send(resourceType + ".create", "pm", report);
    }

    private ServiceMessage createReportMessage(ServiceMessage event,
                                               String resourceType,
                                               Resource resource) {
        ServiceMessage report = new ServiceMessage();
        report.setActionIdentity(event.getActionIdentity());
        report.setOperationIdentity(event.getOperationIdentity());
        if (resource != null) {
            report.setObjRef("http://" + applicationName + "/" + resourceType + "/" + resource.getId());
        }
        Boolean eventSuccess = (Boolean) event.getParam("success");
        if (eventSuccess == null) {
            report.addParam("success", true);
        } else {
            report.addParam("success", eventSuccess);
        }

        return report;
    }

    private String getTaskExecutorRoutingKey(Resource resource) {
        ServerStorable serverStorable = (ServerStorable) resource;
        String serverName = staffRcClient.getServerById(serverStorable.getServerId()).getName();
        String serverShortName = serverName.split(".")[0];

        return "te" + "." + serverShortName;
    }

}
