package ru.majordomo.hms.rc.user.resourceProcessor.impl.website;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.amqp.WebSiteAMQPController;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.*;

import static ru.majordomo.hms.rc.user.common.Constants.PM;

@Slf4j
public class WebsiteUpdateFromPm extends BaseWebsiteProcessor {

    public WebsiteUpdateFromPm(WebSiteAMQPController processorContext, StaffResourceControllerClient staffRcClient) {
        super(processorContext, staffRcClient);
    }

    @Override
    public void process(ResourceActionContext<WebSite> context) throws Exception {
        ServiceMessage serviceMessage = context.getMessage();
        WebSite resource;

        String resourceId = (String) serviceMessage.getParam("resourceId");
        if (resourceId == null || resourceId.equals("")) {
            throw new ParameterValidationException("Не указан resourceId");
        }
        try {
            resource = processorContext.getGovernor().build(resourceId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Не найден ресурс с ID: " + resourceId);
        }
        if (serviceMessage.getParam("lock") != null) {
            resource.setLocked((Boolean) serviceMessage.getParam("lock"));
            processorContext.getGovernor().store(resource);
            processorContext.getSender().send(context, PM);
            return;
        }
        if (resource.isLocked()) {
            throw new ParameterValidationException("Ресурс в процессе обновления");
        }

        resource = processorContext.getGovernor().update(serviceMessage);

        context.setResource(resource);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        context.setRoutingKey(routingKey);

        validateAndFullExtendedAction(context);

        resource.setLocked(true);
        processorContext.getGovernor().store(resource);

        processorContext.getSender().send(context, routingKey);
    }
}