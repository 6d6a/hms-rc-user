package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDatabaseUser;

import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigGovernorOfDatabaseUser.class, ConfigStaffResourceControllerClient.class}, webEnvironment = NONE, properties = {
        "default.database.service.name:DATABASE_MYSQL"
})
public class GovernorOfDatabaseUserTest {

    @Autowired
    private GovernorOfDatabaseUser governor;
    private List<DatabaseUser> databaseUsers;

    @Autowired
    private DatabaseUserRepository repository;

    @Value("${default.database.service.name}")
    private String defaultDatabaseService;

    @Before
    public void setUp() throws Exception {
        databaseUsers = ResourceGenerator.generateBatchOfDatabaseUsers();
        repository.save(databaseUsers);
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutAccountId() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateWithoutAccountIdServiceMessage();
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadServiceId() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        serviceMessage.addParam("serviceId", ObjectId.get().toString());
        System.out.println(serviceMessage);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadAllowedAddressList() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateDatabaseUserCreateServiceMessage();
        List<String> allowedAddressList = Arrays.asList("8.8.8.8", "9.9.9.9", "Валера");
        serviceMessage.addParam("allowedAddressList", allowedAddressList);
        System.out.println(serviceMessage);
        governor.create(serviceMessage);
    }

    @Test
    public void build() {

    }

    @Test
    public void drop() throws Exception {
        governor.drop(databaseUsers.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test
    public void buildAll() throws Exception {
        Assert.isTrue(governor.buildAll().size() == 3);
    }

    @Test
    public void buildAllByAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", databaseUsers.get(1).getAccountId());
        Assert.isTrue(governor.buildAll(keyValue).size() == 2);
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.addParam("resourceId", databaseUsers.get(0).getId());
        serviceMessage.setAccountId(databaseUsers.get(0).getAccountId());
        String oldPasswordHash = databaseUsers.get(0).getPasswordHash();
        serviceMessage.addParam("password", "87654321");
        DatabaseUser databaseUser = (DatabaseUser) governor.update(serviceMessage);
        Assert.isTrue(!repository.findOne(databaseUser.getId()).getPasswordHash().equals(oldPasswordHash));
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
    }

}