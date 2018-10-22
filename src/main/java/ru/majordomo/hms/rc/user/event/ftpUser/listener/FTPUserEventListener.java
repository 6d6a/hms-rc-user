package ru.majordomo.hms.rc.user.event.ftpUser.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserCreateEvent;
import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserImportEvent;
import ru.majordomo.hms.rc.user.importing.FTPUserDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.resources.FTPUser;

@Component
@Profile("import")
public class FTPUserEventListener extends ResourceEventListener<FTPUser> {
    @Autowired
    public FTPUserEventListener(
            GovernorOfFTPUser governorOfFTPUser,
            FTPUserDBImportService ftpUserDBImportService
    ) {
        this.governor = governorOfFTPUser;
        this.dbImportService = ftpUserDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(FTPUserCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(FTPUserImportEvent event) {
        processImportEvent(event);
    }
}
