package ru.majordomo.hms.rc.user.event.webSite.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.database.DatabaseCreateEvent;
import ru.majordomo.hms.rc.user.event.database.DatabaseImportEvent;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteCreateEvent;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseDBImportService;
import ru.majordomo.hms.rc.user.importing.WebSiteDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Component
public class WebSiteEventListener {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteEventListener.class);

    private final GovernorOfWebSite governorOfWebSite;
    private final WebSiteDBImportService databaseDBImportService;

    @Autowired
    public WebSiteEventListener(
            GovernorOfWebSite governorOfWebSite,
            WebSiteDBImportService databaseDBImportService) {
        this.governorOfWebSite = governorOfWebSite;
        this.databaseDBImportService = databaseDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(WebSiteCreateEvent event) {
        WebSite webSite = event.getSource();

        logger.debug("We got CreateEvent for: " + webSite);

        try {
            governorOfWebSite.validateAndStore(webSite);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(WebSiteImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got ImportEvent");

        try {
            databaseDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
