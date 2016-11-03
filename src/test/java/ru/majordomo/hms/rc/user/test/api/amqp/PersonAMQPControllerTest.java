package ru.majordomo.hms.rc.user.test.api.amqp;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.BrokerManager;
import ru.majordomo.hms.rc.user.test.config.amqp.ConfigPersonAMQPControllerTest;
import ru.majordomo.hms.rc.user.test.config.amqp.ConfigWebSiteAMQPControllerTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigPersonAMQPControllerTest.class, AMQPBrokerConfig.class}, webEnvironment = RANDOM_PORT)
public class PersonAMQPControllerTest {

    private static BrokerManager brokerManager = new BrokerManager();

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Sender sender;


    @BeforeClass
    public static void startBroker() throws Exception {
        brokerManager.start();
    }

    @Before
    public void setUp() throws Exception {
        setUpTE();
        setUpPM();
    }

    private void setUpTE() throws Exception {
        Exchange exchange = new TopicExchange("person.create");
        Queue queue = new Queue("te", false);
        String routingKey = "te";

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(new Binding(queue.getName(), Binding.DestinationType.QUEUE,
                exchange.getName(), routingKey,
                Collections.<String, Object>emptyMap()));

    }

    private void setUpPM() throws Exception {
        Exchange exchange = new TopicExchange("person.create");
        Queue queue = new Queue("pm", false);
        String routingKey = "pm";

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(new Binding(queue.getName(), Binding.DestinationType.QUEUE,
                exchange.getName(), routingKey,
                Collections.<String, Object>emptyMap()));
    }

    @Test
    public void sendAndReceive() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateBadServiceMessage();
        sender.send("person.create", "rc.user", serviceMessage, "pm");
        Message message = rabbitTemplate.receive("pm", 1000);
    }
}
