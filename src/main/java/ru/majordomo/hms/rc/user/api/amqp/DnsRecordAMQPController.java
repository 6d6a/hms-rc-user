package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;

@EnableRabbit
@Service
public class DnsRecordAMQPController extends BaseAMQPController<DNSResourceRecord> {

    @Autowired
    public void setGovernor(GovernorOfDnsRecord governor) {
        this.governor = governor;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.dns-record.create",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "dns-record.create", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleCreateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleCreateEventFromPM("dns-record", serviceMessage);
                break;
            case ("te"):
                handleCreateEventFromTE("dns-record", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.dns-record.update",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "dns-record.update", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleUpdateEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleUpdateEventFromPM("dns-record", serviceMessage);
                break;
            case ("te"):
                handleUpdateEventFromTE("dns-record", serviceMessage);
                break;
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${spring.application.name}.dns-record.delete",
            durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "dns-record.delete", type = ExchangeTypes.TOPIC),
            key = "rc.user"))
    public void handleDeleteEvent(@Header(value = "provider") String eventProvider,
                                  @Payload ServiceMessage serviceMessage) {
        switch (eventProvider) {
            case ("pm"):
                handleDeleteEventFromPM("dns-record", serviceMessage);
                break;
            case ("te"):
                handleDeleteEventFromTE("dns-record", serviceMessage);
                break;
        }
    }
}
