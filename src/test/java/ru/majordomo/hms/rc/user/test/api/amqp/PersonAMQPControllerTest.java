package ru.majordomo.hms.rc.user.test.api.amqp;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.*;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,

                ConfigAMQPControllers.class,
                AMQPBrokerConfig.class,

                ConfigGovernors.class
        }, webEnvironment = RANDOM_PORT,
        properties = {
                "resources.quotable.warnProcent.mailbox=90"
        }
)
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

    @AfterClass
    public static void stopBroker() throws Exception {
        brokerManager.stop();
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
                Collections.emptyMap()));

    }

    private void setUpPM() throws Exception {
        Exchange exchange = new TopicExchange("person.create");
        Queue queue = new Queue("pm", false);
        String routingKey = "pm";

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(new Binding(queue.getName(), Binding.DestinationType.QUEUE,
                exchange.getName(), routingKey,
                Collections.emptyMap()));
    }

    @Test
    public void sendAndReceive() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generatePersonCreateBadServiceMessage();
        sender.send("person.create", "rc.user", serviceMessage, "pm");
        Message message = rabbitTemplate.receive("pm", 1000);
    }
}
