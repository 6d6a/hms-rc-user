package ru.majordomo.hms.rc.user.event.databaseUser.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserCreateEvent;
import ru.majordomo.hms.rc.user.event.databaseUser.DatabaseUserImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseUserDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@Component
public class DatabaseUserEventListener extends ResourceEventListener<DatabaseUser> {

    @Autowired
    public DatabaseUserEventListener(
            GovernorOfDatabaseUser governorOfDatabaseUser,
            DatabaseUserDBImportService databaseUserDBImportService) {
        this.governor = governorOfDatabaseUser;
        this.dbImportService = databaseUserDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DatabaseUserCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DatabaseUserImportEvent event) {
        processImportEvent(event);
    }
}
