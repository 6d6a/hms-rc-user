package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.PERSON_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.PERSON_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.PERSON_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class PersonAMQPController extends BaseAMQPController<Person> {

    @Autowired
    public void setGovernor(GovernorOfPerson governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + PERSON_CREATE)
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleCreateEventFromPM("person", serviceMessage);
                break;
            case TE:
                handleCreateEventFromTE("person", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + PERSON_UPDATE)
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleUpdateEventFromPM("person", serviceMessage);
                break;
            case TE:
                handleUpdateEventFromTE("person", serviceMessage);
                break;
        }
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + PERSON_DELETE)
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (getRealProviderName(eventProvider)) {
            case PM:
                handleDeleteEventFromPM("person", serviceMessage);
                break;
            case TE:
                handleDeleteEventFromTE("person", serviceMessage);
                break;
        }
    }
}
