package ru.majordomo.hms.rc.user.test.managers;

import org.bouncycastle.asn1.dvcs.Data;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
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
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

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
            Assert.assertEquals("Имя не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabase.getName());
            Assert.assertEquals("Статус включен/выключен не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabase.getName());
            Assert.assertTrue(!buildedDatabase.getDatabaseUsers().isEmpty());
            Assert.assertTrue(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabase.getDatabaseUserIds()));
            Assert.assertEquals("Service не совпадает с ожидаемым", batchOfDatabases.get(0).getServiceId(), buildedDatabase.getServiceId());
            Assert.assertEquals("Quota не совпадает с ожидаемым", batchOfDatabases.get(0).getQuota(), buildedDatabase.getQuota());
            Assert.assertEquals("QuotaUsed не совпадает с ожидаемым", batchOfDatabases.get(0).getQuotaUsed(), buildedDatabase.getQuotaUsed());
            Assert.assertEquals("Type не совпадает с ожидаемым", batchOfDatabases.get(0).getType(), buildedDatabase.getType());
            Assert.assertEquals("Writable не совпадает с ожидаемым", batchOfDatabases.get(0).getWritable(), buildedDatabase.getWritable());
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void buildByKeyValue() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", batchOfDatabases.get(0).getId());
        keyValue.put("accountId", batchOfDatabases.get(0).getAccountId());
        Database buildedDatabaseByDatabaseId = (Database) governor.build(keyValue);
        try {
            Assert.assertEquals("Имя не совпадает", batchOfDatabases.get(0).getName(), buildedDatabaseByDatabaseId.getName());
            Assert.assertEquals("Статус включен/выключен не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabaseByDatabaseId.getName());
            Assert.assertTrue(!buildedDatabaseByDatabaseId.getDatabaseUsers().isEmpty());
            Assert.assertTrue(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabaseByDatabaseId.getDatabaseUserIds()));
            Assert.assertEquals("Service не совпадает с ожидаемым", batchOfDatabases.get(0).getServiceId(), buildedDatabaseByDatabaseId.getServiceId());
            Assert.assertEquals("Quota не совпадает с ожидаемым", batchOfDatabases.get(0).getQuota(), buildedDatabaseByDatabaseId.getQuota());
            Assert.assertEquals("QuotaUsed не совпадает с ожидаемым", batchOfDatabases.get(0).getQuotaUsed(), buildedDatabaseByDatabaseId.getQuotaUsed());
            Assert.assertEquals("Type не совпадает с ожидаемым", batchOfDatabases.get(0).getType(), buildedDatabaseByDatabaseId.getType());
            Assert.assertEquals("Writable не совпадает с ожидаемым", batchOfDatabases.get(0).getWritable(), buildedDatabaseByDatabaseId.getWritable());
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void buildAll() {
        List<Database> buildedDatabases = (List<Database>) governor.buildAll();
        try {
            Assert.assertEquals("Имя не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabases.get(0).getName());
            Assert.assertEquals("Статус включен/выключен не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabases.get(0).getName());
            Assert.assertTrue(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabases.get(0).getDatabaseUserIds()));
            Assert.assertEquals("Service не совпадает с ожидаемым", batchOfDatabases.get(0).getServiceId(), buildedDatabases.get(0).getServiceId());
            Assert.assertEquals("Quota не совпадает с ожидаемым", batchOfDatabases.get(0).getQuota(), buildedDatabases.get(0).getQuota());
            Assert.assertEquals("QuotaUsed не совпадает с ожидаемым", batchOfDatabases.get(0).getQuotaUsed(), buildedDatabases.get(0).getQuotaUsed());
            Assert.assertEquals("Type не совпадает с ожидаемым", batchOfDatabases.get(0).getType(), buildedDatabases.get(0).getType());
            Assert.assertEquals("Writable не совпадает с ожидаемым", batchOfDatabases.get(0).getWritable(), buildedDatabases.get(0).getWritable());
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void buildAllByKyeValue() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", batchOfDatabases.get(0).getAccountId());
        List<Database> buildedDatabasesByAccountId = (List<Database>) governor.buildAll(keyValue);
        try {
            Assert.assertTrue("Количество элментов в списке не совпдает с ожидаемым", buildedDatabasesByAccountId.size() == 1);
            Assert.assertEquals("Имя не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabasesByAccountId.get(0).getName());
            Assert.assertEquals("Статус включен/выключен не совпадает с ожидаемым", batchOfDatabases.get(0).getName(), buildedDatabasesByAccountId.get(0).getName());
            Assert.assertTrue(batchOfDatabases.get(0).getDatabaseUserIds().containsAll(buildedDatabasesByAccountId.get(0).getDatabaseUserIds()));
            Assert.assertEquals("Service не совпадает с ожидаемым", batchOfDatabases.get(0).getServiceId(), buildedDatabasesByAccountId.get(0).getServiceId());
            Assert.assertEquals("Quota не совпадает с ожидаемым", batchOfDatabases.get(0).getQuota(), buildedDatabasesByAccountId.get(0).getQuota());
            Assert.assertEquals("QuotaUsed не совпадает с ожидаемым", batchOfDatabases.get(0).getQuotaUsed(), buildedDatabasesByAccountId.get(0).getQuotaUsed());
            Assert.assertEquals("Type не совпадает с ожидаемым", batchOfDatabases.get(0).getType(), buildedDatabasesByAccountId.get(0).getType());
            Assert.assertEquals("Writable не совпадает с ожидаемым", batchOfDatabases.get(0).getWritable(), buildedDatabasesByAccountId.get(0).getWritable());
        } catch (ParameterValidateException e) {
            e.printStackTrace();
            Assert.fail();
        }
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
        Assert.assertEquals(0, repository.findByDatabaseUserIdsContaining(databaseUserId).size());
    }

    @After
    public void deleteAll() {
        repository.deleteAll();
        databaseUserRepository.deleteAll();
    }
}