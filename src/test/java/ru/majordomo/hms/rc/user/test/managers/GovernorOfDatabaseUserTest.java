package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.*;

import javax.validation.ConstraintViolationException;

import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.hasItem;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class
        },
        webEnvironment = NONE
)
public class GovernorOfDatabaseUserTest {

    @Autowired
    private GovernorOfDatabaseUser governor;
    private List<DatabaseUser> databaseUsers;

    @Autowired
    private DatabaseUserRepository repository;
    @Autowired
    private DatabaseRepository databaseRepository;

    @Before
    public void setUp() throws Exception {
        databaseUsers = ResourceGenerator.generateBatchOfDatabaseUsers();
        repository.saveAll(databaseUsers);
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void createWithDatabaseIds() throws Exception {
        List<Database> databases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database : databases) {
            database.setDatabaseUserIds(Collections.emptyList());
        }
        databaseRepository.saveAll(databases);
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        serviceMessage.setAccountId(databases.get(0).getAccountId());
        serviceMessage.addParam("databaseIds", Collections.singletonList(databases.get(0).getId()));
        System.out.println(databases);
        System.out.println(serviceMessage);
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        DatabaseUser databaseUser = governor.completeOversightAndStore(ovs);

        assertThat(databaseRepository
                .findById(databases.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден")).getDatabaseUserIds(), hasItem(databaseUser.getId()));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutAccountId() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateWithoutAccountIdServiceMessage();
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadServiceId() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        serviceMessage.addParam("serviceId", ObjectId.get().toString());
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithBadAllowedAddressList() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        List<String> allowedAddressList = Arrays.asList("8.8.8.8", "9.9.9.9", "Валера");
        serviceMessage.addParam("allowedAddressList", allowedAddressList);
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithBadDBType() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        serviceMessage.addParam("type", "BAD_DB_TYPE");
        OperationOversight<DatabaseUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void drop() throws Exception {
        List<Database> databases = ResourceGenerator.generateBatchOfDatabases();
        List<String> databaseUserIds = new ArrayList<>();
        for (DatabaseUser databaseUser : databaseUsers) {
            databaseUserIds.add(databaseUser.getId());
        }
        databases.get(0).setDatabaseUserIds(databaseUserIds);
        databaseRepository.saveAll(databases);
        governor.drop(databaseUsers.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test
    public void buildAll() throws Exception {
        assertThat(governor.buildAll().size(), is(3));
    }

    @Test
    public void buildAllByAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", databaseUsers.get(1).getAccountId());
        assertThat(governor.buildAll(keyValue).size(), is(2));
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.addParam("resourceId", databaseUsers.get(0).getId());
        serviceMessage.setAccountId(databaseUsers.get(0).getAccountId());
        String oldPasswordHash = databaseUsers.get(0).getPasswordHash();
        serviceMessage.addParam("password", "87654321");
        OperationOversight<DatabaseUser> ovs = governor.updateByOversight(serviceMessage);
        DatabaseUser databaseUser = governor.completeOversightAndStore(ovs);
        assertThat(repository
                .findById(databaseUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"))
                .getPasswordHash(), not(oldPasswordHash));
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
        databaseRepository.deleteAll();
    }
}