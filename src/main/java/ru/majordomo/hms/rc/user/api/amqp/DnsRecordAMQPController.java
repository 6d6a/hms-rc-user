package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DNS_RECORD_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DNS_RECORD_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DNS_RECORD_UPDATE;

@Service
public class DnsRecordAMQPController extends BaseAMQPController<DNSResourceRecord> {

    @Autowired
    public void setGovernor(GovernorOfDnsRecord governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DNS_RECORD_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DNS_RECORD_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DNS_RECORD_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return "dns-record";
    }
}
