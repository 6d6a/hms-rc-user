package ru.majordomo.hms.rc.user.resourceProcessor.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;

import javax.validation.ConstraintViolationException;

import static ru.majordomo.hms.rc.user.common.Constants.TE;

@AllArgsConstructor
public class DefaultCreatePmProcessor<T extends Resource> implements ResourceProcessor<T> {
    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) {
        OperationOversight<T> ovs = processorContext.getGovernor().createByOversight(context.getMessage());

        context.setOvs(ovs);

        String routingKey = processorContext.getRoutingKeyResolver().get(context);

        if (!TE.equals(routingKey) && !StringUtils.startsWith(routingKey, TE + ".")) {
            try {
                processorContext.getGovernor().completeOversightAndStore(ovs);
            } catch (ParameterValidationException | ConstraintViolationException e) {
                processorContext.getGovernor().removeOversight(ovs);
                throw e;
            }
        }

        processorContext.getSender().send(context, routingKey);
    }
}
