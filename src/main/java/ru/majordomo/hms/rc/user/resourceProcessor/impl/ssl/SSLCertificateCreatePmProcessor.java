package ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessorContext;
import ru.majordomo.hms.rc.user.resourceProcessor.support.ResourceSearcher;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;
import static ru.majordomo.hms.rc.user.common.Constants.PM;

@AllArgsConstructor
public class SSLCertificateCreatePmProcessor implements ResourceProcessor<SSLCertificate> {
    private final ResourceProcessorContext<SSLCertificate> processorContext;
    private final ResourceSearcher<Domain> domainSearcher;

    @Override
    public void process(ResourceActionContext<SSLCertificate> context) {
        ServiceMessage serviceMessage = context.getMessage();

        String name = (String) serviceMessage.getParam("name");
        String accountId = serviceMessage.getAccountId();

        if (name == null || name.trim().isEmpty()) {
            throw new ParameterValidationException("Необходимо указать имя домена в поле name");
        }

        Map<String, String> search = new HashMap<>();
        search.put("name", name);
        search.put("accountId", accountId);

        try {
            domainSearcher.build(search);
        } catch (ResourceNotFoundException e) {
            throw new ParameterValidationException("На аккаунте не найден домен с именем " + name);
        }

        if (((GovernorOfSSLCertificate) processorContext.getGovernor()).canCreateCustomCertificate(serviceMessage)) {
            SSLCertificate certificate = processorContext.getGovernor().create(serviceMessage);

            context.setResource(certificate);

            String teRoutingKey = ((GovernorOfSSLCertificate) processorContext.getGovernor()).getTERoutingKey(certificate);

            processorContext.getSender().send(context, teRoutingKey == null ? PM : teRoutingKey);
        } else {
            SSLCertificate certificate = new SSLCertificate();
            certificate.setAccountId(accountId);
            certificate.setSwitchedOn(true);
            certificate.setName(name);

            context.setResource(certificate);

            processorContext.getSender().send(context, LETSENCRYPT);
        }
    }
}
