package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Optional;

import static ru.majordomo.hms.rc.user.common.Constants.TE;

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

        Optional<OperationOversight<T>> existedOvs = processorContext.getGovernor().getOperationOversightByResource(resource);
        if (existedOvs.isPresent()) {
            throw new ParameterValidationException("Ресурс в процессе обновления");
        }

        OperationOversight<T> ovs = processorContext.getGovernor().updateByOversight(context.getMessage());
        context.setOvs(ovs);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (!TE.equals(routingKey)) {
            processorContext.getGovernor().completeOversightAndStore(ovs);
        }

        processorContext.getSender().send(context, routingKey);
    }
}
