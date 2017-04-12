package ru.majordomo.hms.rc.user.event.ftpUser.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserCreateEvent;
import ru.majordomo.hms.rc.user.event.ftpUser.FTPUserImportEvent;
import ru.majordomo.hms.rc.user.importing.FTPUserDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.resources.FTPUser;

@Component
public class FTPUserEventListener {
    private final static Logger logger = LoggerFactory.getLogger(FTPUserEventListener.class);

    private final GovernorOfFTPUser governorOfFTPUser;
    private final FTPUserDBImportService ftpUserDBImportService;

    @Autowired
    public FTPUserEventListener(
            GovernorOfFTPUser governorOfFTPUser,
            FTPUserDBImportService ftpUserDBImportService) {
        this.governorOfFTPUser = governorOfFTPUser;
        this.ftpUserDBImportService = ftpUserDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onFTPUserCreateEvent(FTPUserCreateEvent event) {
        FTPUser ftpUser = event.getSource();

        logger.debug("We got FTPUserCreateEvent");

        try {
            governorOfFTPUser.validateAndStore(ftpUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onFTPUserImportEvent(FTPUserImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got FTPUserImportEvent");

        try {
            ftpUserDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
