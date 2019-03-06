package ru.majordomo.hms.rc.user.resourceProcessor.impl.te;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;

@AllArgsConstructor
public class TeCreateProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) {
        ServiceMessage serviceMessage = context.getMessage();

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");

        String resourceUrl = serviceMessage.getObjRef();

        T resource = processorContext.getResourceByUrlBuilder().get(resourceUrl);

        context.setResource(resource);

        if (resource != null) {
            if (!successEvent) {
                processorContext.getGovernor().drop(resource.getId());
            } else {
                resource.setLocked(false);
                processorContext.getGovernor().store(resource);
            }
        }

        processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
    }
}
