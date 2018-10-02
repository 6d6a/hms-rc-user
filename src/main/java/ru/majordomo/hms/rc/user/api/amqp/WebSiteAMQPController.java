package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
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

@Service
public class WebSiteAMQPController extends BaseAMQPController<WebSite> {

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return "website";
    }
}
