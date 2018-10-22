package ru.majordomo.hms.rc.user.event.database.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.database.DatabaseCreateEvent;
import ru.majordomo.hms.rc.user.event.database.DatabaseImportEvent;
import ru.majordomo.hms.rc.user.importing.DatabaseDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.resources.Database;

@Component
@Profile("import")
public class DatabaseEventListener extends ResourceEventListener<Database> {

    @Autowired
    public DatabaseEventListener(
            GovernorOfDatabase governorOfDatabase,
            DatabaseDBImportService databaseDBImportService) {
        this.governor = governorOfDatabase;
        this.dbImportService = databaseDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(DatabaseCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(DatabaseImportEvent event) {
        processImportEvent(event);
    }
}
