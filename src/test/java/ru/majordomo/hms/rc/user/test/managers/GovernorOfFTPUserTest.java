package ru.majordomo.hms.rc.user.test.managers;

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
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfFTPUser;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfUnixAccount;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigGovernorOfFTPUser.class, ConfigGovernorOfUnixAccount.class, ConfigStaffResourceControllerClient.class}, webEnvironment = NONE)
public class GovernorOfFTPUserTest {

    @Autowired
    private GovernorOfFTPUser governor;
    private List<FTPUser> ftpUsers;
    private List<UnixAccount> unixAccounts;

    @Autowired
    private FTPUserRepository repository;

    @Autowired
    private UnixAccountRepository unixAccountRepository;

    @Before
    public void setUp() throws Exception {
        unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        unixAccountRepository.save(unixAccounts);
        ftpUsers = ResourceGenerator.generateBatchOfFTPUsersWithUnixAccountId(unixAccounts.get(0).getId());
        repository.save(ftpUsers);
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
        unixAccountRepository.deleteAll();
    }


    @Test
    public void buildByKeyValue() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", ftpUsers.get(0).getId());
        keyValue.put("accountId", ftpUsers.get(0).getAccountId());
        FTPUser ftpUser = (FTPUser) governor.build(keyValue);
        Assert.assertEquals(ftpUser.getId(), ftpUsers.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void buildByKeyValueWithoutResourceId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", ftpUsers.get(0).getAccountId());
        governor.build(keyValue);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void buildKeyValueWithoutAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", ftpUsers.get(0).getId());
        governor.build(keyValue);
    }

    @Test
    public void buildAllByKeyValueWithBadAccountId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", ObjectId.get().toString());

        Collection<? extends Resource> ftpUsers = governor.buildAll(keyValue);
        Assert.assertEquals(0, ftpUsers.size());
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.addParam("unixAccountId", unixAccounts.get(1).getId());
        serviceMessage.setAccountId(unixAccounts.get(1).getAccountId());
        governor.create(serviceMessage);
        List<FTPUser> ftpUsers = repository.findByAccountId(unixAccounts.get(1).getAccountId());
        Assert.assertEquals(1, ftpUsers.size());
        Assert.assertEquals("f111111", ftpUsers.get(0).getName());
        Assert.assertEquals("/mjru", ftpUsers.get(0).getHomeDir());
        Assert.assertNotNull(ftpUsers.get(0).getPasswordHash());
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutUnixAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutHomeDir() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("homedir");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutPassword() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("password");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("name");
        governor.create(serviceMessage);
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        String oldPasswordHash = ftpUsers.get(0).getPasswordHash();
        serviceMessage.setAccountId(ftpUsers.get(0).getAccountId());
        serviceMessage.addParam("resourceId", ftpUsers.get(0).getId());
        serviceMessage.addParam("switchedOn", false);
        serviceMessage.delParam("name");
        governor.update(serviceMessage);
        FTPUser ftpUser = repository.findOne(ftpUsers.get(0).getId());
        Assert.assertEquals("/mjru", ftpUser.getHomeDir());
        Assert.assertFalse(ftpUser.getSwitchedOn());
        Assert.assertNotEquals(oldPasswordHash, ftpUser.getPasswordHash());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateNonExistentResourceId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(unixAccounts.get(0).getAccountId());
        serviceMessage.addParam("resourceId", ObjectId.get().toString());
        serviceMessage.delParam("name");
        governor.update(serviceMessage);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateNonExistentAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", ftpUsers.get(0).getId());
        serviceMessage.delParam("name");
        governor.update(serviceMessage);
    }

    @Test
    public void drop() throws Exception {
        governor.drop(ftpUsers.get(0).getId());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }
}
