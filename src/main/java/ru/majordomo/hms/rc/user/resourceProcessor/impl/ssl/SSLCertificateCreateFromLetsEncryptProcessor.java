package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.PM;

@AllArgsConstructor
public class SSLCertificateCreateFromLetsEncryptProcessor implements ResourceProcessor<SSLCertificate> {
    private final ResourceProcessorContext<SSLCertificate> processorContext;

    @Override
    public void process(ResourceActionContext<SSLCertificate> context) {
        ServiceMessage serviceMessage = context.getMessage();

        Boolean success = (Boolean) serviceMessage.getParam("success");

        if (success) {
            context.setResource(
                    processorContext.getGovernor().create(serviceMessage)
            );

            processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
        } else {
            processorContext.getSender().send(context, PM);
        }
    }
}
