package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
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
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("unix-account", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("unix-account", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("unix-account", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("unix-account", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("unix-account", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("unix-account", serviceMessage);
                break;
        }
    }
}
