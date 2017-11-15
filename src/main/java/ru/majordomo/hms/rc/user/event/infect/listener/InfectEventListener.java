package ru.majordomo.hms.rc.user.event.infect.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.infect.UnixAccountInfectEvent;
import ru.majordomo.hms.rc.user.resources.MalwareReport;

@Component
public class InfectEventListener {
    private PmFeignClient personmgr;

    private static final String FILENAMES = "files";
    private static final String PARAMETRS_KEY = "parametrs";
    private static final String API_NAME_KEY = "api_name";
    private static final String CONFIRM_URL_KEY = "confirm_url";
    private static final String TYPE_KEY = "type";
    private static final String EMAIL = "EMAIL";
    private static final String SMS = "SMS";
    private static final String PRIORITY_KEY = "priority";

    @Autowired
    public InfectEventListener(
            PmFeignClient pmFeignClient
    ) {
        personmgr = pmFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onInfectEvent(UnixAccountInfectEvent event) {
        MalwareReport report = event.getSource();
        convertAndSendEmail(report);
    }

    private void convertAndSendEmail(MalwareReport report) {
        ServiceMessage message = new ServiceMessage();
        message.addParam(API_NAME_KEY, "MajordomoVHMalwareFound");
        message.addParam(CONFIRM_URL_KEY, "https://hms.majordomo.ru/quarantine");
        message.addParam(TYPE_KEY, EMAIL);
        message.addParam(PRIORITY_KEY, 10);
        personmgr.sendNotificationToClient(message);
    }
}
