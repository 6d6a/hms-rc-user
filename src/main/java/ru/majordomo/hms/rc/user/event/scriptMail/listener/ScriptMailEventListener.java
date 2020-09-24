package ru.majordomo.hms.rc.user.event.scriptMail.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.scriptMail.UnixAccountScriptMailNotifyEvent;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import static ru.majordomo.hms.rc.user.common.Constants.*;

@Component
public class ScriptMailEventListener {
    private PmFeignClient personmgr;
    
    @Autowired
    public ScriptMailEventListener(
            PmFeignClient pmFeignClient
    ) {
        personmgr = pmFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onScriptMailEvent(UnixAccountScriptMailNotifyEvent event) {
        String accountId = event.getSource();
        convertAndSendEmail(accountId);
    }

    private void convertAndSendEmail(String accountId) {
        ServiceMessage message = new ServiceMessage();
        message.setAccountId(accountId);
        message.addParam(API_NAME_KEY, "MajordomoVHScriptMailDisabled");
        message.addParam(TYPE_KEY, EMAIL);
        message.addParam(PRIORITY_KEY, 10);
        personmgr.sendNotificationToClient(message);
    }
}
