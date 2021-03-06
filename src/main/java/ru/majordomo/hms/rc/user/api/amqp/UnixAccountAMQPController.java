package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.UNIX_ACCOUNT_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.UNIX_ACCOUNT_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.UNIX_ACCOUNT_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class UnixAccountAMQPController extends BaseAMQPController<UnixAccount> {

    @Autowired
    public void setGovernor(GovernorOfUnixAccount governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return Constants.Exchanges.Resource.UNIX_ACCOUNT;
    }

    @Override
    protected String getRoutingKey(ResourceActionContext<UnixAccount> context) {
        String routingKey = getDefaultRoutingKey();

        if (context.getEventProvider().equals(PM)) {
            routingKey = getTaskExecutorRoutingKey(getResourceFromOvsContext(context));
        } else if (context.getEventProvider().equals(TE)) {
            routingKey = getDefaultRoutingKey();
        }

        return routingKey;
    }
}
