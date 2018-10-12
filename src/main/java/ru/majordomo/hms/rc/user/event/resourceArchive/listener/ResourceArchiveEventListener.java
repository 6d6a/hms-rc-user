package ru.majordomo.hms.rc.user.event.resourceArchive.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.resourceArchive.ResourceArchiveCleanEvent;
import ru.majordomo.hms.rc.user.event.resourceArchive.ResourceArchivesCleanEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.schedulers.ResourceArchiveScheduler;

@Component
@Slf4j
public class ResourceArchiveEventListener {
    private final GovernorOfResourceArchive governorOfResourceArchive;
    private Sender sender;
    private ResourceArchiveScheduler scheduler;
    private StaffResourceControllerClient staffRcClient;
    private String applicationName;

    @Autowired
    public ResourceArchiveEventListener(
            GovernorOfResourceArchive governorOfResourceArchive,
            Sender sender,
            ResourceArchiveScheduler scheduler,
            StaffResourceControllerClient staffRcClient,
            @Value("${spring.application.name}") String applicationName
    ) {
        this.scheduler = scheduler;
        this.governorOfResourceArchive = governorOfResourceArchive;
        this.sender = sender;
        this.applicationName = applicationName;
        this.staffRcClient = staffRcClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ResourceArchivesCleanEvent event) {
        log.debug("We got ResourceArchivesCleanEvent");

        scheduler.cleanResourceArchives();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ResourceArchiveCleanEvent event) {
        log.debug("We got ResourceArchiveCleanEvent");

        try {
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("resourceId", event.getSource());
            ResourceArchive archive;

            try {
                archive = governorOfResourceArchive.build(keyValue);
            } catch (ResourceNotFoundException | ParameterValidationException e) {
                log.error("[ResourceArchiveCleanEvent] ResourceArchive not found or not valid (id: " + event.getSource() + ") exception: " + e.getMessage());

                return;
            }

            ServiceMessage report = new ServiceMessage();
            report.setObjRef("http://" + applicationName + "/resource-archive/" + archive.getId());
            report.setAccountId(archive.getAccountId());
            report.addParam("resourceId", archive.getId());

            String serverName = staffRcClient.getServerByServiceId(archive.getServiceId()).getName();

            String teRoutingKey = "te" + "." + serverName.split("\\.")[0];

            log.info("trying to send: " + report + " to: " + teRoutingKey);
            sender.send("resource-archive.delete", teRoutingKey, report);

            archive.setLocked(true);
            governorOfResourceArchive.store(archive);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
