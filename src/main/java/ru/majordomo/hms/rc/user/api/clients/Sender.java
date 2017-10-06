package ru.majordomo.hms.rc.user.api.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;

@Service
public class Sender {

    private final static Logger logger = LoggerFactory.getLogger(Sender.class);

    private RabbitTemplate rabbitTemplate;
    private String instanceName;
    private String fullApplicationName;

    @Autowired
    public Sender(
            RabbitTemplate rabbitTemplate,
            @Value("${spring.application.name}") String applicationName,
            @Value("${hms.instance.name}") String instanceName
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.instanceName = instanceName;
        this.fullApplicationName = instanceName + "." + applicationName;
    }

    public void send(String exchange, String routingKey, ServiceMessage payload) {
        send(exchange, routingKey, payload, fullApplicationName);
    }

    public void send(String exchange, String routingKey, ServiceMessage payload, String provider) {
        if (!routingKey.startsWith(instanceName + ".")) {
            routingKey = instanceName + "." + routingKey;
        }

        if (!provider.startsWith(instanceName + ".")) {
            provider = instanceName + "." + provider;
        }

        Message message = buildMessage(payload, provider);
        rabbitTemplate.send(exchange, routingKey, message);
        logger.info("ACTION_IDENTITY: " + payload.getActionIdentity() +
                " OPERATION_IDENTITY: " + payload.getOperationIdentity() +
                " Сообщение от: " + provider + " " +
                "в exchange: " + exchange + " " +
                "с routing key: " + routingKey + " " +
                "отправлено." + " " +
                "Вот оно: " + message.toString());
    }

    private Message buildMessage(ServiceMessage payload, String provider) {
        return MessageBuilder
                .withBody(payload.toJson().getBytes())
                .setContentType("application/json")
                .setHeader("provider", provider)
                .build();
    }
}
