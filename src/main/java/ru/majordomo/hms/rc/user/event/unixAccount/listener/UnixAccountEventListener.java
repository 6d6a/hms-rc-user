package ru.majordomo.hms.rc.user.event.unixAccount.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.event.ResourceEventListener;
import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountCreateEvent;
import ru.majordomo.hms.rc.user.event.unixAccount.UnixAccountImportEvent;
import ru.majordomo.hms.rc.user.importing.UnixAccountDBImportService;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Component
public class UnixAccountEventListener extends ResourceEventListener<UnixAccount> {
    @Autowired
    public UnixAccountEventListener(
            GovernorOfUnixAccount governorOfUnixAccount,
            UnixAccountDBImportService unixAccountDBImportService) {
        this.governor = governorOfUnixAccount;
        this.dbImportService = unixAccountDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCreateEvent(UnixAccountCreateEvent event) {
        processCreateEvent(event);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onImportEvent(UnixAccountImportEvent event) {
        processImportEvent(event);
    }
}
