package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

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
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.TE;

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

        SSLCertificate sslCertificate = processorContext.getGovernor().build(keyValue);

        OperationOversight<SSLCertificate> ovs = processorContext.getGovernor().dropByOversight(sslCertificate.getId());
        context.setOvs(ovs);
        processorContext.getGovernor().completeOversightAndDelete(ovs);

        serviceMessage.addParam("success", true);

        processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));
    }
}
