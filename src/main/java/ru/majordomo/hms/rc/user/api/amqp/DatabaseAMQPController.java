package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.resources.Database;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class DatabaseAMQPController extends BaseAMQPController<Database> {

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_CREATE)
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("database", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("database", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("database", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("database", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("database", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("database", serviceMessage);
                break;
        }
    }
}
