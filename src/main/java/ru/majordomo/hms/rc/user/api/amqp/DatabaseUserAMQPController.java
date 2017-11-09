package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_USER_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_USER_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_USER_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class DatabaseUserAMQPController extends BaseAMQPController<DatabaseUser> {

    @Autowired
    public void setGovernor(GovernorOfDatabaseUser governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("database-user", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("database-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("database-user", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("database-user", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("database-user", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("database-user", serviceMessage);
                break;
        }
    }
}
