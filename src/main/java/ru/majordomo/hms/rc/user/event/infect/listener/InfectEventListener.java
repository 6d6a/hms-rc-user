package ru.majordomo.hms.rc.user.event.infect.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.infect.UnixAccountInfectNotifyEvent;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import static ru.majordomo.hms.rc.user.common.Constants.*;

@Component
public class InfectEventListener {
    private PmFeignClient personmgr;

    @Autowired
    public InfectEventListener(
            PmFeignClient pmFeignClient
    ) {
        personmgr = pmFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onInfectEvent(UnixAccountInfectNotifyEvent event) {
        String accountId = event.getSource();
        convertAndSendEmail(accountId);
    }

    private void convertAndSendEmail(String accountId) {
        ServiceMessage message = new ServiceMessage();
        message.setAccountId(accountId);
        message.addParam(API_NAME_KEY, "MajordomoVHMalwareFound");
        message.addParam(TYPE_KEY, EMAIL);
        message.addParam(PRIORITY_KEY, 10);
        personmgr.sendNotificationToClient(message);
    }
}
