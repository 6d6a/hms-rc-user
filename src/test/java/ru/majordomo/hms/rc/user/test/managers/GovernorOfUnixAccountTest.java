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
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,

                ConfigGovernors.class
        },
        webEnvironment = NONE)
public class GovernorOfUnixAccountTest {
    @Autowired
    private GovernorOfUnixAccount governor;
    @Autowired
    private UnixAccountRepository repository;
    @Autowired
    private Cleaner cleaner;

    private List<UnixAccount> unixAccounts;

    @Before
    public void setUp() throws Exception {
        unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        repository.save(unixAccounts);
    }

    @After
    public void cleanUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateServiceMessage();
        UnixAccount unixAccount = (UnixAccount) governor.create(serviceMessage);
        System.out.println(unixAccount.toString());
    }

    @Test
    public void createWithQuotaAsInt() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateQuotaIntServiceMessage();
        UnixAccount unixAccount = (UnixAccount) governor.create(serviceMessage);
        assertThat(unixAccount.getQuota(), is(10485760L));
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutQuota() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateServiceMessage();
        serviceMessage.delParam("quota");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithQuotaAsString() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateQuotaStringServiceMessage();
        governor.create(serviceMessage);
    }

    @Test
    public void getFreeUidWhenNoOneUsed() throws Exception {
        repository.deleteAll();
        assertThat(governor.getFreeUid(), is(governor.MIN_UID));
    }

    @Test
    public void getFreeUidWhenAllUpperUidUsed() throws Exception {
        repository.deleteAll();
        for (int i = (governor.MAX_UID - 3); i <= governor.MAX_UID; i++) {
            UnixAccount unixAccount = new UnixAccount();
            unixAccount.setUid(i);
            repository.save(unixAccount);
        }

        assertThat(governor.getFreeUid(), is(governor.MIN_UID));
    }

    @Test
    public void nameToInteger() throws Exception {
        String name = "u134035";
        Integer nameId = governor.getUnixAccountNameAsInteger(name);
        assertThat(nameId, is(134035));
    }

    @Test(expected = ParameterValidateException.class)
    public void notNumNameToInteger() throws Exception {
        String name = "non-num-name";
        governor.getUnixAccountNameAsInteger(name);
    }

    @Test
    public void nameIsNumerable() throws Exception {
        assertThat(governor.nameIsNumerable("u134035"), is(true));
    }

    @Test
    public void nameIsNotNumerable() throws Exception {
        assertThat(governor.nameIsNumerable("u134035a"), is(false));
    }

    @Test
    public void getFreeNumNameWhenNoOneUsed() throws Exception {
        assertThat(governor.getFreeUnixAccountName(), is("u" + governor.MIN_UID));
    }

    @Test
    public void getFreeNumNameWhenOnlyOneAccAndItsNameU2000() throws Exception {
        repository.deleteAll();
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setName("u2000");
        repository.save(unixAccount);
        assertThat(governor.getFreeUnixAccountName(), is("u" + governor.MAX_UID));
    }

    @Test
    public void freeNumName() throws Exception {
        repository.deleteAll();
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setName("u134035");
        repository.save(unixAccount);

        unixAccount = new UnixAccount();
        unixAccount.setName("u134037");
        repository.save(unixAccount);

        System.out.println(repository.findAll());
        assertThat(governor.getFreeUnixAccountName(), is("u134036"));
    }

    @Test
    public void lowUidNotValid() throws Exception {
        assertThat(governor.isUidValid(1000), is(false));
    }

    @Test
    public void uidValid() throws Exception {
        assertThat(governor.isUidValid(2000), is(true));
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(unixAccounts.get(0).getAccountId());
        serviceMessage.addParam("resourceId", unixAccounts.get(0).getId());
        serviceMessage.addParam("keyPair", "GENERATE");
        CronTask cronTask = new CronTask();
        cronTask.setCommand("php ./index.php");
        cronTask.setExecTime("* 1 1 1 1");
        serviceMessage.addParam("crontab", Arrays.asList(cronTask));
        SSHKeyPair keyPair = unixAccounts.get(0).getKeyPair();
        System.out.println(unixAccounts.get(0).getCrontab());
        governor.update(serviceMessage);
        UnixAccount unixAccount = repository.findOne(unixAccounts.get(0).getId());
        assertThat(unixAccount.getKeyPair().toString(), not(keyPair.toString()));
        System.out.println(unixAccount.getCrontab());
    }

    @Test
    public void sendmailBlock() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(unixAccounts.get(0).getAccountId());
        serviceMessage.addParam("resourceId", unixAccounts.get(0).getId());
        serviceMessage.addParam("sendmailAllowed", false);
        governor.update(serviceMessage);
        UnixAccount unixAccount = repository.findOne(unixAccounts.get(0).getId());
        assertThat(unixAccount.getSendmailAllowed(), is(false));
    }
}
