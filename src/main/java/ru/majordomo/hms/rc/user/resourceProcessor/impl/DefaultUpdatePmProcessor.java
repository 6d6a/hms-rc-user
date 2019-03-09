package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;

@AllArgsConstructor
public class DefaultUpdatePmProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) throws Exception {
        ServiceMessage serviceMessage = context.getMessage();
        T resource;

        String resourceId = (String) serviceMessage.getParam("resourceId");
        if (resourceId == null || resourceId.equals("")) {
            throw new ParameterValidationException("Не указан resourceId");
        }
        try {
            resource = processorContext.getGovernor().build(resourceId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Не найден ресурс с ID: " + resourceId);
        }
        if (resource.isLocked()) {
            throw new ParameterValidationException("Ресурс в процессе обновления");
        }

        resource = processorContext.getGovernor().update(serviceMessage);

        context.setResource(resource);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (
                (resource instanceof ServerStorable || resource instanceof Serviceable)
                        && !(resource instanceof Mailbox)
        ) {
            resource.setLocked(true);
            processorContext.getGovernor().store(resource);
        }

        processorContext.getSender().send(context, routingKey);
    }
}
