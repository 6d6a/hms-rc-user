package ru.majordomo.hms.rc.user.resourceProcessor.impl.te;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Optional;

@AllArgsConstructor
public class TeCreateProcessor<T extends Resource> implements ResourceProcessor<T> {
    /**
     * Получаем сообщение из TE о результатах CREATE ресурса.
     * В случае успеха создаём ресурс в соответствующей коллекции.
     * В случае неудачи - создания объекта не происходит
     */

    private final ResourceProcessorContext<T> processorContext;

    @Override
    public void process(ResourceActionContext<T> context) {
        ServiceMessage serviceMessage = context.getMessage();

        Boolean successEvent = (Boolean) serviceMessage.getParam("success");
        String ovsId = (String) serviceMessage.getParam("ovsId");

        Optional<OperationOversight<T>> ovs = processorContext.getOperationOversightBuilder().get(ovsId);

        //Устанавливаем для Resolver'ов, если вдруг к нам пришёл только ovsId
        ovs.ifPresent(context::setOvs);

        if (ovs.isPresent()) {
            if (successEvent) {
                processorContext.getGovernor().completeOversightAndStore(ovs.get());
            } else {
                processorContext.getGovernor().removeOversight(ovs.get());
            }
        }

        processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
    }
}
