package ru.majordomo.hms.rc.user.test.api.amqp;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.BrokerManager;
import ru.majordomo.hms.rc.user.test.config.amqp.ConfigWebSiteAMQPControllerTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigWebSiteAMQPControllerTest.class, AMQPBrokerConfig.class}, webEnvironment = RANDOM_PORT)
public class WebSiteAMQPControllerTest {

    private static BrokerManager brokerManager = new BrokerManager();

    private ServiceMessage serviceMessage;

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

    }
}