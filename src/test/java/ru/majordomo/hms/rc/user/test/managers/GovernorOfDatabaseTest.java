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
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDatabase;

import java.util.*;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigGovernorOfDatabase.class, ConfigStaffResourceControllerClient.class}, webEnvironment = NONE, properties = {
        "default.database.service.name:DATABASE_MYSQL"
})
public class GovernorOfDatabaseTest {
    @Autowired
    private GovernorOfDatabase governor;

    @Autowired
    private DatabaseUserRepository databaseUserRepository;

    @Autowired
    private DatabaseRepository repository;

    private List<Database> batchOfDatabases;

    @Before
    public void setUp() throws Exception {
        batchOfDatabases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database: batchOfDatabases) {
            for (DatabaseUser databaseUser : database.getDatabaseUsers()) {
                databaseUserRepository.save(databaseUser);
            }
            repository.save(database);
        }
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(batchOfDatabases.get(0).getDatabaseUserIds());
        governor.create(serviceMessage);
    }

    @Test
    public void createWithQuotaNotLong() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(batchOfDatabases.get(0).getDatabaseUserIds());
        serviceMessage.addParam("quota", 5);
        governor.create(serviceMessage);
    }

    @Test
    public void createWithQuotaUsedNotLong() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(batchOfDatabases.get(0).getDatabaseUserIds());
        serviceMessage.addParam("quota", 10);
        serviceMessage.addParam("quotaUsed", 5);
        governor.create(serviceMessage);
    }

    @Test
    public void drop() throws Exception {
        governor.drop(batchOfDatabases.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNotExisted() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test
    public void createWithoutDatabaseUsers() {
        List<String> emptyDatabaseUsers = new ArrayList<>();
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(emptyDatabaseUsers);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutAccountId() {
        String emptyString = "";
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(batchOfDatabases.get(0).getDatabaseUserIds());
        serviceMessage.setAccountId(emptyString);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithWrongDBtype() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseCreateServiceMessage(batchOfDatabases.get(0).getDatabaseUserIds());
        serviceMessage.delParam("type");
        serviceMessage.addParam("type", "WRONGDBTYPE");
        governor.create(serviceMessage);
    }

    @Test
    public void build() {
        Database buildedDatabase = (Database) governor.build(batchOfDatabases.get(0).getId());
        try {
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabase.getName()));
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabase.getName()));
            assertThat(buildedDatabase.getDatabaseUsers(), not(Collections.emptyList()));
            assertThat(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabase.getDatabaseUserIds()), is(true));
            assertThat(batchOfDatabases.get(0).getServiceId(), is(buildedDatabase.getServiceId()));
            assertThat(batchOfDatabases.get(0).getQuota(), is(buildedDatabase.getQuota()));
            assertThat(batchOfDatabases.get(0).getQuotaUsed(), is(buildedDatabase.getQuotaUsed()));
            assertThat(batchOfDatabases.get(0).getType(), is(buildedDatabase.getType()));
            assertThat(batchOfDatabases.get(0).getWritable(), is(buildedDatabase.getWritable()));
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void buildByKeyValue() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", batchOfDatabases.get(0).getId());
        keyValue.put("accountId", batchOfDatabases.get(0).getAccountId());
        Database buildedDatabaseByDatabaseId = (Database) governor.build(keyValue);
        try {
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabaseByDatabaseId.getName()));
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabaseByDatabaseId.getName()));
            assertThat(buildedDatabaseByDatabaseId.getDatabaseUsers(), not(Collections.emptyList()));
            assertThat(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabaseByDatabaseId.getDatabaseUserIds()), is(true));
            assertThat(batchOfDatabases.get(0).getServiceId(), is(buildedDatabaseByDatabaseId.getServiceId()));
            assertThat(batchOfDatabases.get(0).getQuota(), is(buildedDatabaseByDatabaseId.getQuota()));
            assertThat(batchOfDatabases.get(0).getQuotaUsed(), is(buildedDatabaseByDatabaseId.getQuotaUsed()));
            assertThat(batchOfDatabases.get(0).getType(), is(buildedDatabaseByDatabaseId.getType()));
            assertThat(batchOfDatabases.get(0).getWritable(), is(buildedDatabaseByDatabaseId.getWritable()));
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void buildAll() {
        List<Database> buildedDatabases = (List<Database>) governor.buildAll();
        try {
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabases.get(0).getName()));
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabases.get(0).getName()));
            assertThat(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabases.get(0).getDatabaseUserIds()), is(true));
            assertThat(batchOfDatabases.get(0).getServiceId(), is(buildedDatabases.get(0).getServiceId()));
            assertThat(batchOfDatabases.get(0).getQuota(), is(buildedDatabases.get(0).getQuota()));
            assertThat(batchOfDatabases.get(0).getQuotaUsed(), is(buildedDatabases.get(0).getQuotaUsed()));
            assertThat(batchOfDatabases.get(0).getType(), is(buildedDatabases.get(0).getType()));
            assertThat(batchOfDatabases.get(0).getWritable(), is(buildedDatabases.get(0).getWritable()));
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void buildAllByKyeValue() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", batchOfDatabases.get(0).getAccountId());
        List<Database> buildedDatabasesByAccountId = (List<Database>) governor.buildAll(keyValue);
        try {
            assertThat(buildedDatabasesByAccountId.size(), is(1));
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabasesByAccountId.get(0).getName()));
            assertThat(batchOfDatabases.get(0).getName(), is(buildedDatabasesByAccountId.get(0).getName()));
            assertThat(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabasesByAccountId.get(0).getDatabaseUserIds()), is(true));
            assertThat(batchOfDatabases.get(0).getServiceId(), is(buildedDatabasesByAccountId.get(0).getServiceId()));
            assertThat(batchOfDatabases.get(0).getQuota(), is(buildedDatabasesByAccountId.get(0).getQuota()));
            assertThat(batchOfDatabases.get(0).getQuotaUsed(), is(buildedDatabasesByAccountId.get(0).getQuotaUsed()));
            assertThat(batchOfDatabases.get(0).getType(), is(buildedDatabasesByAccountId.get(0).getType()));
            assertThat(batchOfDatabases.get(0).getWritable(), is(buildedDatabasesByAccountId.get(0).getWritable()));
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void buildAllByDatabaseUserId() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("databaseUserId", batchOfDatabases.get(0).getDatabaseUserIds().get(0));
        List<Database> buildedDatabasesByAccountId = (List<Database>) governor.buildAll(keyValue);
        assertThat(batchOfDatabases.get(0).getDatabaseUserIds().get(0), is(buildedDatabasesByAccountId.get(0).getDatabaseUserIds().get(0)));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void buildWithWrongId() {
        String resourceId = ObjectId.get().toString();
        governor.build(resourceId);
    }

    @Test
    public void removeUserFromDatabase() {
        Database database = repository.findOne(batchOfDatabases.get(0).getId());
        String databaseUserId = database.getDatabaseUserIds().get(0);
        governor.removeDatabaseUserIdFromDatabases(databaseUserId);
        assertThat(repository.findByDatabaseUserIdsContaining(databaseUserId).size(), is(0));
    }

    @After
    public void deleteAll() {
        repository.deleteAll();
        databaseUserRepository.deleteAll();
    }
}