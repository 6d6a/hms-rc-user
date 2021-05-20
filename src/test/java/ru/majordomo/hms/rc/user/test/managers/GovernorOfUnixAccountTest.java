package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.model.Counter;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.service.CounterService;
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

import java.util.Collections;
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
                ValidationConfig.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class
        },
        webEnvironment = NONE
)
public class GovernorOfUnixAccountTest {
    @Autowired
    private GovernorOfUnixAccount governor;
    @Autowired
    private UnixAccountRepository repository;
    @Autowired
    private Cleaner cleaner;
    @Autowired
    private MongoOperations mongoOperations;

    private List<UnixAccount> unixAccounts;

    @Before
    public void setUp() throws Exception {
        unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        repository.saveAll(unixAccounts);
    }

    @After
    public void cleanUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateServiceMessage();
        OperationOversight<UnixAccount> ovs = governor.createByOversight(serviceMessage);
        UnixAccount unixAccount = governor.completeOversightAndStore(ovs);
        System.out.println(unixAccount.toString());
    }

    @Test
    public void createWithQuotaAsInt() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateQuotaIntServiceMessage();
        OperationOversight<UnixAccount> ovs = governor.createByOversight(serviceMessage);
        UnixAccount unixAccount = governor.completeOversightAndStore(ovs);
        assertThat(unixAccount.getQuota(), is(10485760L));
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithoutQuota() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateServiceMessage();
        serviceMessage.delParam("quota");
        OperationOversight<UnixAccount> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithQuotaAsString() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateUnixAccountCreateQuotaStringServiceMessage();
        OperationOversight<UnixAccount> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void getFreeUidWhenNoOneUsed() throws Exception {
        repository.deleteAll();
        mongoOperations.remove(
                new Query(
                        new Criteria("internalName").is(CounterService.UNIX_ACCOUNT_UID_INTERNAL_NAME)
                ),
                Counter.class
        );

        assertThat(governor.getFreeUid(), is(CounterService.DEFAULT_START_UID + 1));
    }

    @Test
    public void nameToInteger() throws Exception {
        String name = "u134035";
        Integer nameId = governor.getUnixAccountNameAsInteger(name);
        assertThat(nameId, is(134035));
    }

    @Test
    public void everyNextUidIsIncreasesByOne() {
        Integer uid = governor.getFreeUid();
        for (int i = 0; i < 10; i++) {
            assertThat(governor.getFreeUid(), is(++uid));
        }
    }

    @Test
    public void ifNextUidIsBusyGetNextOne() {
        Integer uid = governor.getFreeUid();

        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setName("djgkasjdgjasdg");
        unixAccount.setUid(uid + 1);
        repository.insert(unixAccount);

        assertThat(governor.getFreeUid(), is(uid + 2));
    }

    @Test
    public void getFreeNameByAccountIdIsNull() {
        Integer uid = governor.getFreeUid();
        assertThat(governor.getFreeUnixAccountName(null), is("u" + (uid+1)));
    }

    @Test
    public void getFreeNameByAccountIdIsShit() {
        Integer uid = governor.getFreeUid();

        assertThat(
                governor.getFreeUnixAccountName("!@#$%^&*(()_+"),
                is("u" + (++uid))
        );

        assertThat(
                governor.getFreeUnixAccountName("dkjghjaksjg"),
                is("u" + (++uid))
        );

        assertThat(
                governor.getFreeUnixAccountName("ac_82475"),
                is("u" + (++uid))
        );

        assertThat(
                governor.getFreeUnixAccountName(""),
                is("u" + (++uid))
        );

        assertThat(
                governor.getFreeUnixAccountName(" \t \n"),
                is("u" + (++uid))
        );
    }

    @Test
    public void getFreeNameByAccountIdIsStringOfNumberAndItsNotBusy() {
        assertThat(
                governor.getFreeUnixAccountName("135135"),
                is("u135135")
        );
    }

    @Test
    public void getFreeNameByAccountIdIsStringOfNumberAndItsBusy() {
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setName("u235135");
        unixAccount.setUid(235135);
        repository.insert(unixAccount);

        assertThat(
                governor.getFreeUnixAccountName("235135"),
                is("u235135_1")
        );
    }

    @Test(expected = ParameterValidationException.class)
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
        assertThat(
                governor.getFreeUnixAccountName(null),
                is("u" + (CounterService.DEFAULT_START_UID + 1))
        );
    }

    @Test
    public void getFreeNumNameWhenOnlyOneAccAndItsNameU70001() throws Exception {
        repository.deleteAll();
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setName("u70001");
        repository.save(unixAccount);
        assertThat(governor.getFreeUnixAccountName(null), is("u" + (CounterService.DEFAULT_START_UID + 2)));
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
        serviceMessage.addParam("crontab", Collections.singletonList(cronTask));
        SSHKeyPair keyPair = unixAccounts.get(0).getKeyPair();
        System.out.println(unixAccounts.get(0).getCrontab());
        OperationOversight<UnixAccount> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
        UnixAccount unixAccount = repository
                .findById(unixAccounts.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
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
        OperationOversight<UnixAccount> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
        UnixAccount unixAccount = repository
                .findById(unixAccounts.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertThat(unixAccount.getSendmailAllowed(), is(false));
    }
}
