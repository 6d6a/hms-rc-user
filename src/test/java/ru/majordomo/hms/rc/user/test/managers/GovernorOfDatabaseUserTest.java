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
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDatabaseUser;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigGovernorOfDatabaseUser.class, ConfigStaffResourceControllerClient.class}, webEnvironment = NONE, properties = {})
public class GovernorOfDatabaseUserTest {

    @Autowired
    private GovernorOfDatabaseUser governor;
    private List<DatabaseUser> databaseUsers;

    @Autowired
    private DatabaseUserRepository repository;

    @Before
    public void setUp() throws Exception {
        databaseUsers = ResourceGenerator.generateBatchOfDatabaseUsers();
        System.out.println(databaseUsers.toString());
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

    @Test
    public void drop() throws Exception {
        governor.drop(databaseUsers.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test
    public void dropByAccountId() throws Exception {
        governor.dropByAccountId(databaseUsers.get(1).getId(), databaseUsers.get(1).getAccountId());
        if (repository.count() != 2) {
            throw new Exception("Количество оставшихся аккаунтов не равно ожидаемому");
        }
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropByNotOwnedAccountId() throws Exception {
        try {
            governor.dropByAccountId(databaseUsers.get(1).getId(), databaseUsers.get(0).getAccountId());
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @Test
    public void buildAll() throws Exception {
        Collection<? extends Resource> result = governor.buildAll();
        if (result.size() != 3) {
            throw new Exception("Количество аккаунтов не равно ожидаемому");
        }
    }

    @Test
    public void buildAllByAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", databaseUsers.get(1).getAccountId());
        Collection<? extends Resource> result = governor.buildAll(keyValue);
        if (result.size() != 2) {
            throw new Exception("Количество аккаунтов не равно ожидаемому");
        }
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
    }

}