package ru.majordomo.hms.rc.user.event.databaseUser.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserCreateEvent;
import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseUserDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@Component
public class DatabaseUserEventListener {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseUserEventListener.class);

    private final GovernorOfDatabaseUser governorOfDatabaseUser;
    private final DatabaseUserDBImportService databaseUserDBImportService;

    @Autowired
    public DatabaseUserEventListener(
            GovernorOfDatabaseUser governorOfDatabaseUser,
            DatabaseUserDBImportService databaseUserDBImportService) {
        this.governorOfDatabaseUser = governorOfDatabaseUser;
        this.databaseUserDBImportService = databaseUserDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DatabaseUserCreateEvent event) {
        DatabaseUser databaseUser = event.getSource();

        logger.debug("We got CreateEvent");

        try {
            governorOfDatabaseUser.validateAndStore(databaseUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DatabaseUserImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got ImportEvent");

        try {
            databaseUserDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
