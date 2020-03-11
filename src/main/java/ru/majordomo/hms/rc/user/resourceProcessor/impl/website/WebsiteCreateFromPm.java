package ru.majordomo.hms.rc.user.resourceProcessor.impl.website;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.amqp.WebSiteAMQPController;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Slf4j
public class WebsiteCreateFromPm extends BaseWebsiteProcessor {

    public WebsiteCreateFromPm(WebSiteAMQPController processorContext, StaffResourceControllerClient staffRcClient) {
        super(processorContext, staffRcClient);
    }

    @Override
    public void process(ResourceActionContext<WebSite> context) throws Exception {
        WebSite resource = processorContext.getGovernor().create(context.getMessage());

        context.setResource(resource);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);
        context.setRoutingKey(routingKey);

        validateAndFullExtendedAction(context);

        resource.setLocked(true);
        processorContext.getGovernor().store(resource);

        processorContext.getSender().send(context, routingKey);
    }
}
