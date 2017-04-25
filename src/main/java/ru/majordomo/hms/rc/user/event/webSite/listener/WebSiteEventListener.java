package ru.majordomo.hms.rc.user.event.webSite.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteCreateEvent;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteImportEvent;
import ru.majordomo.hms.rc.user.importing.WebSiteDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Component
public class WebSiteEventListener extends ResourceEventListener<WebSite> {

    @Autowired
    public WebSiteEventListener(
            GovernorOfWebSite governorOfWebSite,
            WebSiteDBImportService databaseDBImportService
    ) {
        this.governor = governorOfWebSite;
        this.dbImportService = databaseDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(WebSiteCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(WebSiteImportEvent event) {
        processImportEvent(event);
    }
}
