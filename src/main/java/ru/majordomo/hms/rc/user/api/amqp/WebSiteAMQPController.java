package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class WebSiteAMQPController extends BaseAMQPController<WebSite> {

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + WEBSITE_CREATE)
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("website", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("website", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + WEBSITE_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("website", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("website", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + WEBSITE_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("website", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("website", serviceMessage);
                break;
        }
    }
}
