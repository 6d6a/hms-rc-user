package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfRedirect;
import ru.majordomo.hms.rc.user.resources.Redirect;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.REDIRECT_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.REDIRECT_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.REDIRECT_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.Resource;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class RedirectAMQPController extends BaseAMQPController<Redirect> {

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
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return Resource.REDIRECT;
    }

    @Override
    protected String getRoutingKey(ResourceActionContext<Redirect> context) {
        if (context.getEventProvider().equals(PM)) {
            return getTaskExecutorRoutingKey(context.getResource());
        } else if (context.getEventProvider().equals(TE)) {
            return getDefaultRoutingKey();
        } else {
            return getDefaultRoutingKey();
        }
    }
}
