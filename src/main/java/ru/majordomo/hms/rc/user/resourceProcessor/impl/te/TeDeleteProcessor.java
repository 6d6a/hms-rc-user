package ru.majordomo.hms.rc.user.resourceProcessor.impl.te;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;

@AllArgsConstructor
public class TeDeleteProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) {
        ServiceMessage serviceMessage = context.getMessage();

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");

        String resourceUrl = serviceMessage.getObjRef();

        context.setResource(
                processorContext.getResourceByUrlBuilder().get(resourceUrl)
        );

        if (context.getResource() != null) {
            if (successEvent){
                processorContext.getGovernor().drop(context.getResource().getId());
            } else {
                context.getResource().setLocked(false);
                processorContext.getGovernor().store(context.getResource());
            }
        }

        processorContext.getSender().send(
                context, processorContext.getRoutingKeyResolver().get(context)
        );
    }
}
