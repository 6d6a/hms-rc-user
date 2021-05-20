package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;
import static ru.majordomo.hms.rc.user.common.Constants.PM;

@Slf4j
@AllArgsConstructor
public class SSLCertificateUpdateFromPm implements ResourceProcessor<SSLCertificate> {
    private final ResourceProcessorContext<SSLCertificate> processorContext;

    @Override
    public void process(ResourceActionContext<SSLCertificate> context) throws Exception {
        ServiceMessage serviceMessage = context.getMessage();

        OperationOversight<SSLCertificate> ovs = processorContext.getGovernor().updateByOversight(serviceMessage);
        context.setOvs(ovs);
        processorContext.getGovernor().completeOversightAndStore(ovs);

        String routingKey;

        if ("LETS_ENCRYPT".equals(serviceMessage.getParam("renew"))) {
            routingKey = LETSENCRYPT;
        } else {
            String teRoutingKey = ((GovernorOfSSLCertificate) processorContext.getGovernor()).getTERoutingKey(ovs.getResource());

            if (teRoutingKey == null) {
                routingKey = PM;
            } else {
                routingKey = teRoutingKey;
            }
        }

        processorContext.getGovernor().validateAndStore(ovs.getResource());

        processorContext.getSender().send(context, routingKey);
    }
}
