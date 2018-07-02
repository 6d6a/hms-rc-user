package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.RESOURCE_ARCHIVE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.RESOURCE_ARCHIVE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.RESOURCE_ARCHIVE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class ResourceArchiveAMQPController extends BaseAMQPController<ResourceArchive> {

    @Autowired
    public void setGovernor(GovernorOfResourceArchive governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("resource-archive", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("resource-archive", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("resource-archive", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("resource-archive", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("resource-archive", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("resource-archive", serviceMessage);
                break;
        }
    }
}
