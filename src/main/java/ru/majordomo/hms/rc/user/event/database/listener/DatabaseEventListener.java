package ru.majordomo.hms.rc.user.event.database.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.database.DatabaseCreateEvent;
import ru.majordomo.hms.rc.user.event.database.DatabaseImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.resources.Database;

@Component
public class DatabaseEventListener {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseEventListener.class);

    private final GovernorOfDatabase governorOfDatabase;
    private final DatabaseDBImportService databaseDBImportService;

    @Autowired
    public DatabaseEventListener(
            GovernorOfDatabase governorOfDatabase,
            DatabaseDBImportService databaseDBImportService) {
        this.governorOfDatabase = governorOfDatabase;
        this.databaseDBImportService = databaseDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDatabaseCreateEvent(DatabaseCreateEvent event) {
        Database database = event.getSource();

        logger.debug("We got DatabaseCreateEvent");

        try {
            governorOfDatabase.validateAndStore(database);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onDatabaseImportEvent(DatabaseImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got DatabaseImportEvent");

        try {
            databaseDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
