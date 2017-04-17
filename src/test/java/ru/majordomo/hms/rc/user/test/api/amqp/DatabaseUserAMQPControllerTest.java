package ru.majordomo.hms.rc.user.test.api.amqp;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.BrokerManager;
import ru.majordomo.hms.rc.user.test.config.amqp.ConfigAMQPControllers;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.Collections;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        ConfigStaffResourceControllerClient.class,
        ConfigDomainRegistrarClient.class,

        FongoConfig.class,
        RedisConfig.class,
        DatabaseConfig.class,
        ValidationConfig.class,

        ConfigAMQPControllers.class,
        AMQPBrokerConfig.class,

        ConfigGovernors.class
}, webEnvironment = RANDOM_PORT)
public class DatabaseUserAMQPControllerTest {

    private static BrokerManager brokerManager = new BrokerManager();

    private List<DatabaseUser> databaseUserList;

    @Autowired
    DatabaseUserRepository repository;

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
        setUpTE("create");
        setUpPM("create");
        setUpTE("delete");
        setUpPM("delete");
        setUpTE("update");
        setUpPM("update");
        databaseUserList = ResourceGenerator.generateBatchOfDatabaseUsers();
        repository.save(databaseUserList);
    }

    @After
    public void delete() {
        repository.deleteAll();
    }

    private void setUpTE(String actionString) throws Exception {
        Exchange exchange = new TopicExchange("database-user." + actionString);

        Queue queue = new Queue("te.web100500", false);
        String routingKey = "te.web100500";

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(
                new Binding(
                        queue.getName(),
                        Binding.DestinationType.QUEUE,
                        exchange.getName(),
                        routingKey,
                        Collections.emptyMap()
                )
        );
    }

    private void setUpPM(String actionString) throws Exception {
        Exchange exchange = new TopicExchange("database-user." + actionString);

        Queue queue = new Queue("pm-" + actionString, false);
        String routingKey = "pm";

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(
                new Binding(
                        queue.getName(),
                        Binding.DestinationType.QUEUE,
                        exchange.getName(),
                        routingKey,
                        Collections.emptyMap()
                )
        );
    }

    @Test
    public void sendAndReceiveCreatePM() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        serviceMessage.addParam("serviceId", "583300c5a94c541d14d58c85");
        sender.send("database-user.create", "rc.user", serviceMessage, "pm");
        Message message = rabbitTemplate.receive("te.web100500", 3000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }

    @Test
    public void sendAndReceiveCreateTE() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateReportFromTEServiceMessage();
        serviceMessage.setObjRef("http://rc-user/database-user/" + databaseUserList.get(0).getId());
        sender.send("database-user.create", "rc.user", serviceMessage, "te");
        Message message = rabbitTemplate.receive("pm-create", 1000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }

    @Test
    public void sendAndReceiveUpdatePM() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserUpdateServiceMessage();
        serviceMessage.addParam("resourceId", databaseUserList.get(0).getId());
        serviceMessage.setAccountId(databaseUserList.get(0).getAccountId());
        sender.send("database-user.update", "rc.user", serviceMessage, "pm");
        Message message = rabbitTemplate.receive("te.web100500", 1000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }

    @Test
    public void sendAndReceiveUpdateTE() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateReportFromTEServiceMessage();
        serviceMessage.setObjRef("http://rc-user/database-user/" + databaseUserList.get(0).getId());
        sender.send("database-user.update", "rc.user", serviceMessage, "te");
        Message message = rabbitTemplate.receive("pm-update", 1000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }

    @Test
    public void sendAndReceiveDeletePM() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserDeleteServiceMessage();
        serviceMessage.addParam("resourceId", databaseUserList.get(0).getId());
        serviceMessage.setAccountId(databaseUserList.get(0).getAccountId());
        sender.send("database-user.delete", "rc.user", serviceMessage, "pm");
        Message message = rabbitTemplate.receive("te.web100500", 1000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }

    @Test
    public void sendAndReceiveDeleteTE() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateReportFromTEServiceMessage();
        serviceMessage.setObjRef("http://rc-user/database-user/" + databaseUserList.get(0).getId());
        sender.send("database-user.delete", "rc.user", serviceMessage, "te");
        Message message = rabbitTemplate.receive("pm-delete", 1000);
        Assert.notNull(message, "The message must not be null");
        Assert.notNull(message.getBody(), "The message body must not be null");
    }
}
