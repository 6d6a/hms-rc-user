package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;

@AllArgsConstructor
public class DefaultDeletePmProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) throws Exception {
        ServiceMessage serviceMessage = context.getMessage();

        final String accountId = serviceMessage.getAccountId();

        String resourceId = null;


        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = serviceMessage.getParam("resourceId").toString();
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        keyValue.put("resourceId", resourceId);

        T resource = processorContext.getGovernor().build(keyValue);

        context.setResource(resource);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (resource instanceof ServerStorable || resource instanceof Serviceable) {
            processorContext.getGovernor().preDelete(resource.getId());
            resource.setLocked(true);
            processorContext.getGovernor().store(resource);
        } else {
            processorContext.getGovernor().drop(resource.getId());
        }

        processorContext.getSender().send(context, routingKey);
    }
}
