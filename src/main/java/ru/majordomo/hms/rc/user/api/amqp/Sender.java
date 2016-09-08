package ru.majordomo.hms.rc.user.api.amqp;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;

@Service
public class Sender {
    @Autowired
    RabbitTemplate rabbitTemplate;

    private final static Logger logger = LoggerFactory.getLogger(Sender.class);

    public void send(String exchange, String routingKey, ServiceMessage payload) {
        Message message = MessageBuilder
                .withBody(payload.toJson().getBytes())
                .setContentType("application/json")
                .setHeader("provider","rc-user")
                .build();
        logger.warn(message.toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
