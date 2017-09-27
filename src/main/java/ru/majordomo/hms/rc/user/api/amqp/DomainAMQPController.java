package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class DomainAMQPController extends BaseAMQPController<Domain> {

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + DOMAIN_CREATE)
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("domain", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("domain", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + DOMAIN_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("domain", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("domain", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + DOMAIN_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("domain", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("domain", serviceMessage);
                break;
        }
    }
}
