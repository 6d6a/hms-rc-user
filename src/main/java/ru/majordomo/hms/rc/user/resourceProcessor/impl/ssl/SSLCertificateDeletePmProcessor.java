package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

@AllArgsConstructor
public class SSLCertificateDeletePmProcessor implements ResourceProcessor<SSLCertificate> {
    private final ResourceProcessorContext<SSLCertificate> processorContext;

    @Override
    public void process(ResourceActionContext<SSLCertificate> context) {
        ServiceMessage serviceMessage = context.getMessage();

        String resourceId = (String) serviceMessage.getParam("resourceId");
        String accountId = serviceMessage.getAccountId();

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        keyValue.put("resourceId", resourceId);

        context.setResource(
                processorContext.getGovernor().build(keyValue)
        );

        processorContext.getGovernor().drop(context.getResource().getId());

        serviceMessage.addParam("success", true);

        processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
    }
}
