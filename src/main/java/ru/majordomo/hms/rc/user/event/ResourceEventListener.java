package ru.majordomo.hms.rc.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.majordomo.hms.rc.user.importing.ResourceDBImportService;
import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.resources.Resource;

public abstract class ResourceEventListener<T extends Resource> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected LordOfResources<T> governor;
    protected ResourceDBImportService dbImportService;

    protected void processCreateEvent(ResourceCreateEvent<T> event) {
        logger.debug("We got CreateEvent");

        try {
            governor.validateAndStore(event.getSource());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processCreateEventNoValidation(ResourceCreateEvent<T> event) {
        logger.debug("We got CreateEvent [NoValidation]");

        try {
            governor.store(event.getSource());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processImportEvent(ResourceImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got ImportEvent");

        try {
            dbImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
