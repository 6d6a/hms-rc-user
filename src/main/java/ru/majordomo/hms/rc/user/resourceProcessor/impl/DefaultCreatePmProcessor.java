package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;

@AllArgsConstructor
public class DefaultCreatePmProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) {
        T resource = processorContext.getGovernor().create(context.getMessage());

        context.setResource(resource);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (resource instanceof ServerStorable || resource instanceof Serviceable) {
            resource.setLocked(true);
            processorContext.getGovernor().store(resource);
        }

        processorContext.getSender().send(context, routingKey);
    }
}
