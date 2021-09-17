package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.CertificateHelper;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.PM;

@Slf4j
@AllArgsConstructor
public class SSLCertificateUpdateFromLetsEncrypt implements ResourceProcessor<SSLCertificate> {
    private final ResourceProcessorContext<SSLCertificate> processorContext;

    @Override
    public void process(ResourceActionContext<SSLCertificate> context) {
        ServiceMessage serviceMessage = context.getMessage();

        String accountId = serviceMessage.getAccountId();
        String name = (String) serviceMessage.getParam("name");
        Boolean success = (Boolean) serviceMessage.getParam("success");

        Map<String, String> search = new HashMap<>();
        search.put("accountId", accountId);
        search.put("name", name);

        SSLCertificate certificate = processorContext.getGovernor().build(search);

        context.setResource(certificate);

        if (success) {
            ((GovernorOfSSLCertificate) processorContext.getGovernor()).setCustomCertDataAndValidateIt(certificate, serviceMessage);

            processorContext.getGovernor().validateAndStore(certificate);

            processorContext.getSender().send(context, processorContext.getRoutingKeyResolver().get(context));

            log.info("[SSLRoutingKeyUPDATEHook] Сообщение из letsencrypt об успешном апдейте отправлено в: " + processorContext.getRoutingKeyResolver().get(context) + " Контекст: " + context);
        } else {
            try {
                LocalDateTime notAfter = CertificateHelper.getNotAfter(certificate);
                if (notAfter.isBefore(LocalDateTime.now())) {
                    OperationOversight<SSLCertificate> ovs = processorContext.getGovernor().dropByOversight(certificate.getId());
                    context.setOvs(ovs);
                    processorContext.getGovernor().completeOversightAndDelete(ovs);
                }
            } catch (Exception e) {
                log.info("catch e {} e.message {} serviceMessage {}", e.getClass(), e.getMessage(), serviceMessage);
            }

            processorContext.getSender().send(context, PM);
        }
    }
}
