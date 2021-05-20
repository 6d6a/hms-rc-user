package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.Serviceable;

import static ru.majordomo.hms.rc.user.common.Constants.TE;

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

        Optional<OperationOversight<T>> existedOvs = processorContext.getGovernor().getOperationOversightByResource(resource);
        if (existedOvs.isPresent()) {
            throw new ParameterValidationException("Ресурс в процессе обновления");
        }

        OperationOversight<T> ovs = processorContext.getGovernor().dropByOversight(resource.getId());
        context.setOvs(ovs);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (!TE.equals(routingKey)) {
            processorContext.getGovernor().completeOversightAndDelete(ovs);
        }

        processorContext.getSender().send(context, routingKey);
    }
}
