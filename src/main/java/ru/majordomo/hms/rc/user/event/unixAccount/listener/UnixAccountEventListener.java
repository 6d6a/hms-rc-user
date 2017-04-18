package ru.majordomo.hms.rc.user.event.unixAccount.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountCreateEvent;
import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountImportEvent;
import ru.majordomo.hms.rc.user.importing.UnixAccountDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Component
public class UnixAccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(UnixAccountEventListener.class);

    private final GovernorOfUnixAccount governorOfUnixAccount;
    private final UnixAccountDBImportService unixAccountDBImportService;

    @Autowired
    public UnixAccountEventListener(
            GovernorOfUnixAccount governorOfUnixAccount,
            UnixAccountDBImportService unixAccountDBImportService) {
        this.governorOfUnixAccount = governorOfUnixAccount;
        this.unixAccountDBImportService = unixAccountDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onUnixAccountCreateEvent(UnixAccountCreateEvent event) {
        UnixAccount unixAccount = event.getSource();

        logger.debug("We got UnixAccountCreateEvent");

        try {
            governorOfUnixAccount.validateAndStore(unixAccount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onUnixAccountImportEvent(UnixAccountImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got UnixAccountImportEvent");

        try {
            unixAccountDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
