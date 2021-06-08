package ru.majordomo.hms.rc.user.resourceProcessor.impl.domain;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.amqp.DomainAMQPController;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.util.Optional;

@AllArgsConstructor
public class DomainTeCreateProcessor implements ResourceProcessor<Domain> {
    /**
     * Важно! См. коммент к
     * @see DomainAMQPController
     */

    private final ResourceProcessorContext<Domain> processorContext;

    @Override
    public void process(ResourceActionContext<Domain> context) {
        ServiceMessage serviceMessage = context.getMessage();

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String ovsId = (String) serviceMessage.getParam("ovsId");

        Optional<OperationOversight<Domain>> ovs = processorContext.getOperationOversightBuilder().get(ovsId);

        ovs.ifPresent(context::setOvs);

        if (ovs.isPresent()) {
            if (successEvent) {
                processorContext.getGovernor().completeOversightAndStore(ovs.get());
            } else {
                processorContext.getGovernor().removeOversight(ovs.get());
            }
        }

        if (!successEvent) { //переопределяем параметры, перед отправкой в PM
            serviceMessage.addParam("success", true);
            serviceMessage.delParam("errorMessage");
        }

        processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
    }
}
