package ru.majordomo.hms.rc.user.event.quota.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaWarnEvent;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaFullEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.*;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.rc.user.common.Constants.*;

@Component
public class QuotableEventListener {
    private final Logger logger = LoggerFactory.getLogger(QuotableEventListener.class);

    private final PmFeignClient pmFeignClient;

    @Autowired
    public QuotableEventListener(
        PmFeignClient pmFeignClient
    ){
        this.pmFeignClient = pmFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onMailboxQuotaAlmostFullEvent(MailboxQuotaWarnEvent event) {
        Mailbox resource = event.getSource();

        String apiName = "HmsVHMajordomoMailboxQuotaWarn";

        ServiceMessage message = buildServiceMessageFromResource(resource, apiName, EMAIL, 10);

        sendNotificationMessageThroughPM(message);

        logger.info("Send notification in MailboxQuotaWarnEvent with message: " + message);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onMailboxQuotaFullEvent(MailboxQuotaFullEvent event) {
        Mailbox resource = event.getSource();

        String apiName = "HmsVHMajordomoMailboxQuotaFull";

        ServiceMessage message = buildServiceMessageFromResource(resource, apiName, EMAIL, 10);

        sendNotificationMessageThroughPM(message);

        logger.info("Send notification in MailboxQuotaFullEvent with message: " + message);
    }

    private <T extends Quotable> Map<String, String> getQuotableParamsForServiceMessage(T quotable) {
        Long freeQuotaInMb = (quotable.getQuota() - quotable.getQuotaUsed()) / BYTES_IN_ONE_MEBIBYTE;

        Map<String, String> params = new HashMap<>();

        Long quotaInMb = quotable.getQuota() / BYTES_IN_ONE_MEBIBYTE;
        params.put(RESOURCE_KEY, quotable.getClass().getSimpleName());
        params.put(QUOTA_KEY, quotaInMb.toString());
        params.put(FREE_QUOTA_KEY, freeQuotaInMb.toString());

        return params;
    }

    private <T extends Resource> ServiceMessage buildServiceMessageFromResource(T resource, String apiName, String notificationType, int priority) {

        ServiceMessage message = new ServiceMessage();

        message.setAccountId(resource.getAccountId());

        String name = getNameFromResource(resource);

        Map<String, String> params = new HashMap<>();
        params.put(NAME_KEY, name);
        params.putAll(getQuotableParamsForServiceMessage((Quotable) resource));

        message.addParam(PARAMETRS_KEY, params);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(TYPE_KEY, notificationType);
        message.addParam(PRIORITY_KEY, priority);

        return message;
    }

    private String getNameFromResource(Resource resource){
        String name;
        if (resource instanceof Mailbox) {
            name = ((Mailbox) resource).getFullName();
        } else if (resource instanceof Database || resource instanceof UnixAccount) {
            name = resource.getName();
        } else {
            throw new ParameterValidationException("Not implemented " + resource.toString());
        }
        return name;
    }

    private void sendNotificationMessageThroughPM(ServiceMessage message) {
        pmFeignClient.sendNotificationToClient(message);
    }
}

