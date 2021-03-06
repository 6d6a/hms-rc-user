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
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
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

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.anything;

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
        unixAccountRepository.saveAll(unixAccounts);
        ftpUsers = ResourceGenerator.generateBatchOfFTPUsersWithUnixAccountId(unixAccounts.get(0).getId());
        repository.saveAll(ftpUsers);
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
        FTPUser ftpUser = governor.build(keyValue);
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
        assertThat(ftpUsers.size(), is(0));
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.addParam("unixAccountId", unixAccounts.get(1).getId());
        serviceMessage.setAccountId(unixAccounts.get(1).getAccountId());
        serviceMessage.addParam("allowedIPAddresses", Arrays.asList("3.3.3.3", "4.4.4.4"));
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
        List<FTPUser> ftpUsers = repository.findByAccountId(unixAccounts.get(1).getAccountId());
        assertThat(ftpUsers.size(), is(1));
        assertThat(ftpUsers.get(0).getName(), is("f_-111111"));
        assertThat(ftpUsers.get(0).getHomeDir(), is("mjru"));
        assertThat(ftpUsers.get(0).getPasswordHash(), anything());
        assertThat(ftpUsers.get(0).getAllowedIPAddresses(), is(Arrays.asList("3.3.3.3", "4.4.4.4")));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutUnixAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutHomeDir() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("homedir");
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithHomeDirOutsideUserHomeDir() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.addParam("unixAccountId", unixAccounts.get(1).getId());
        serviceMessage.setAccountId(unixAccounts.get(1).getAccountId());
        serviceMessage.delParam("homedir");
        serviceMessage.addParam("homedir", "../../");
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void createWithValidHomeDirContainingUpperDirs() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.addParam("unixAccountId", unixAccounts.get(1).getId());
        serviceMessage.setAccountId(unixAccounts.get(1).getAccountId());
        serviceMessage.delParam("homedir");
        serviceMessage.addParam("homedir", "some_site/www/../../another_site/www");
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutPassword() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("password");
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("name");
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithExistedName() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", ftpUsers.get(0).getName());
        OperationOversight<FTPUser> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        String oldPasswordHash = ftpUsers.get(0).getPasswordHash();
        serviceMessage.setAccountId(ftpUsers.get(0).getAccountId());
        serviceMessage.addParam("resourceId", ftpUsers.get(0).getId());
        serviceMessage.addParam("switchedOn", false);
        serviceMessage.delParam("name");
        serviceMessage.addParam("allowedIPAddresses", Arrays.asList("1.1.1.1", "2.2.2.2"));
        OperationOversight<FTPUser> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
        FTPUser ftpUser = repository
                .findById(ftpUsers.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("???????????? ???? ????????????"));
        assertThat(ftpUser.getHomeDir(), is("mjru"));
        assertThat(ftpUser.getSwitchedOn(), is(false));
        assertThat(ftpUser.getPasswordHash(), not(oldPasswordHash));
        assertThat(ftpUser.getAllowedIPAddresses(), is(Arrays.asList("1.1.1.1", "2.2.2.2")));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateNonExistentResourceId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(unixAccounts.get(0).getAccountId());
        serviceMessage.addParam("resourceId", ObjectId.get().toString());
        serviceMessage.delParam("name");
        OperationOversight<FTPUser> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateNonExistentAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateFTPUserCreateServiceMessageWithoutUnixAccountId();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", ftpUsers.get(0).getId());
        serviceMessage.delParam("name");
        OperationOversight<FTPUser> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void drop() throws Exception {
        governor.drop(ftpUsers.get(0).getId());
        assertNull(repository.findById(ftpUsers.get(0).getId()).orElse(null));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }
}
