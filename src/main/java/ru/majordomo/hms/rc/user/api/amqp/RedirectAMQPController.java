package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfRedirect;
import ru.majordomo.hms.rc.user.resources.Redirect;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.*;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class RedirectAMQPController extends BaseAMQPController<Redirect> {

    private final static String resourceName = "redirect";

    @Autowired
    public void setGovernor(GovernorOfRedirect governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM(resourceName, serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE(resourceName, serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM(resourceName, serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE(resourceName, serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM(resourceName, serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE(resourceName, serviceMessage);
                break;
        }
    }
}
