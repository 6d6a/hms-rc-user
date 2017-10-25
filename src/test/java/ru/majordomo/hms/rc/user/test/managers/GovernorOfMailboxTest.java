package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintViolationException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class
        },
        webEnvironment = NONE,
        properties = {
                "default.redis.host=127.0.0.1",
                "default.mailbox.spamfilter.mood=NEUTRAL",
                "default.mailbox.spamfilter.action=MOVE_TO_SPAM_FOLDER",
                "resources.quotable.warnPercent.mailbox=90"
        }
)
public class GovernorOfMailboxTest {
    @Autowired
    private GovernorOfMailbox governor;
    @Autowired
    private MailboxRepository repository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private MailboxRedisRepository redisRepository;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private List<Mailbox> mailboxes;
    private List<Domain> batchOfDomains;

    private static RedisServer redisServer;

    @Before
    public void setUp() throws Exception {
        if (redisServer == null) {
            JedisConnectionFactory jedisConnectionFactory = (JedisConnectionFactory) redisConnectionFactory;
            redisServer = new RedisServer(jedisConnectionFactory.getPort());
            redisServer.start();
        }
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        batchOfDomains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain : batchOfDomains) {
            Person person = domain.getPerson();
            personRepository.save(person);
        }
        domainRepository.save(batchOfDomains);

        unixAccounts.get(0).setAccountId(batchOfDomains.get(0).getAccountId());
        unixAccountRepository.save(unixAccounts);

        mailboxes = ResourceGenerator.generateBatchOfMailboxesWithDomains(batchOfDomains);

        for (Mailbox mailbox : mailboxes) {
            mailbox.setUid(unixAccounts.get(0).getUid());
            mailbox.setMailSpool("/homebig/" + mailbox.getDomain().getName());
            governor.syncWithRedis(mailbox);
        }

        repository.save(mailboxes);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        redisServer.stop();
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
        redisRepository.deleteAll();
        domainRepository.deleteAll();
        personRepository.deleteAll();
    }

    @Test
    public void redis() throws Exception {
        governor.syncWithRedis(mailboxes.get(0));
        System.out.println(redisTemplate.boundValueOps("mailboxUserData:" + mailboxes.get(0).getFullName()).get());
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        governor.create(serviceMessage);

        Mailbox mailbox = repository.findByNameAndDomainId((String) serviceMessage.getParam("name"), batchOfDomains.get(0).getId());

        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(250000L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getMailFromAllowed(), is(true));
        assertThat(mailbox.getAntiSpamEnabled(), is(false));

        MailboxForRedis redisMailbox = redisRepository.findOne(governor.construct(mailbox).getFullName());
        assertNotNull(redisMailbox);
        assertThat(redisMailbox.getMailFromAllowed(), is(mailbox.getMailFromAllowed()));
        assertThat(redisMailbox.getAntiSpamEnabled(), is(mailbox.getAntiSpamEnabled()));
        assertThat(redisMailbox.getWritable(), is(mailbox.getWritable()));
        assertThat(redisMailbox.getSpamFilterAction(), is(mailbox.getSpamFilterAction()));
        assertThat(redisMailbox.getSpamFilterMood(), is(mailbox.getSpamFilterMood()));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithDuplicateAddress() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", mailboxes.get(0).getName());
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutPassword() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("password");
        governor.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadQuota() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("quota", -1L);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutDomain() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("domainId");
        governor.create(serviceMessage);
    }

    @Test
    public void createFull() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("blackList", Collections.singletonList("ololo@bad.ru"));
        serviceMessage.addParam("whiteList", Collections.singletonList("ololo@good.ru"));
        serviceMessage.addParam("redirectAddresses", Collections.singletonList("ololo@redirect.ru"));
        serviceMessage.addParam("quota", 200L);
        serviceMessage.addParam("spamFilterMood", "NEUTRAL");
        serviceMessage.addParam("spamFilterAction", "MOVE_TO_SPAM_FOLDER");
        governor.create(serviceMessage);

        Mailbox mailbox = governor.construct(repository.findByNameAndDomainId((String) serviceMessage.getParam("name"), batchOfDomains.get(0).getId()));
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(200L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getWhiteList(), is(Collections.singletonList("ololo@good.ru")));
        assertThat(mailbox.getBlackList(), is(Collections.singletonList("ololo@bad.ru")));
        assertThat(mailbox.getRedirectAddresses(), is(Collections.singletonList("ololo@redirect.ru")));
        assertThat(mailbox.getSpamFilterMood(), is(SpamFilterMood.NEUTRAL));
        assertThat(mailbox.getSpamFilterAction(), is(SpamFilterAction.MOVE_TO_SPAM_FOLDER));
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadSpamFilterMood() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("spamFilterMood", "BAD_FILTER_MOOD");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadSpamFilterAction() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("spamFilterAction", "BAD_FILTER_ACTION");
        governor.create(serviceMessage);
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("quota", 500000L);
        serviceMessage.addParam("blackList", Arrays.asList("ololo@bad.ru", "spam.com"));
        serviceMessage.addParam("whiteList", Arrays.asList("good.ru", "ololo@my-friend.com"));
        serviceMessage.addParam("redirectAddresses", Collections.singletonList("ololo@redirect.ru"));
        governor.update(serviceMessage);

        Mailbox mailbox = repository.findOne(mailboxes.get(0).getId());
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(500000L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getWhiteList(), is(Arrays.asList("good.ru", "ololo@my-friend.com")));
        assertThat(mailbox.getBlackList(), is(Arrays.asList("ololo@bad.ru", "spam.com")));
        assertThat(mailbox.getRedirectAddresses(), is(Collections.singletonList("ololo@redirect.ru")));
        assertThat(mailbox.getPasswordHash(), not(mailboxes.get(0).getPasswordHash()));

        MailboxForRedis redisMailbox = redisRepository.findOne(governor.construct(mailbox).getFullName());
        assertNotNull(redisMailbox);
        assertThat(mailbox.getAntiSpamEnabled(), is(redisMailbox.getAntiSpamEnabled()));
        assertThat(String.join(":", mailbox.getWhiteList()), is(redisMailbox.getWhiteList()));
        assertThat(String.join(":", mailbox.getBlackList()), is(redisMailbox.getBlackList()));
        assertThat(mailbox.getWritable(), is(redisMailbox.getWritable()));
        assertThat(String.join(":", mailbox.getRedirectAddresses()), is(redisMailbox.getRedirectAddresses()));
        assertThat(mailbox.getPasswordHash(), is(redisMailbox.getPasswordHash()));
        assertThat(redisMailbox.getServerName(), is("pop100500"));
        Integer uid = mailbox.getUid();
        assertThat(redisMailbox.getStorageData(), is(uid + ":" + uid + ":" + mailbox.getMailSpool()));
    }

    @Test
    @Ignore
    public void updateSetAggregatorAndDropAggregator() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("isAggregator", true);
        governor.update(serviceMessage);

        Mailbox mailbox = repository.findOne(mailboxes.get(0).getId());
        assertThat(mailbox.getIsAggregator(), is(true));

        MailboxForRedis redisMailbox = redisRepository.findOne("*@" + governor.construct(mailbox).getDomain().getName());
        assertNotNull(redisMailbox);
        assertThat(mailbox.getAntiSpamEnabled(), is(redisMailbox.getAntiSpamEnabled()));
        assertThat(String.join(":", mailbox.getWhiteList()), is(redisMailbox.getWhiteList()));
        assertThat(String.join(":", mailbox.getBlackList()), is(redisMailbox.getBlackList()));
        assertThat(mailbox.getWritable(), is(redisMailbox.getWritable()));
        assertThat(String.join(":", mailbox.getRedirectAddresses()), is(redisMailbox.getRedirectAddresses()));
        assertThat(mailbox.getPasswordHash(), is(redisMailbox.getPasswordHash()));
        assertThat(redisMailbox.getServerName(), is("pop100500"));

        serviceMessage.delParam("isAggregator");
        serviceMessage.addParam("isAggregator", false);
        governor.update(serviceMessage);

        assertNull(redisRepository.findOne("*@" + governor.construct(mailbox).getDomain().getName()));
    }

    @Test
    public void updateSetMailFromAllowed() throws Exception {
        ServiceMessage serviceMessage= ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("mailFromAllowed", false);
        governor.update(serviceMessage);
        Mailbox mailbox = repository.findOne(mailboxes.get(0).getId());
        assertThat(mailbox.getMailFromAllowed(), is(false));
        MailboxForRedis redisMailbox = redisRepository.findOne(governor.construct(mailbox).getFullName());
        assertNotNull(redisMailbox);
        assertThat(mailbox.getMailFromAllowed(), is(redisMailbox.getMailFromAllowed()));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateWithoutResourceId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("quota", 300000L);
        governor.update(serviceMessage);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateWithoutAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("quota", 300000L);
        governor.update(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void updateWithBadFilterMood() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("spamFilterMood", "BAD_FILTER_MOOD");
        governor.update(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void updateWithBadFilterAction() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("spamFilterAction", "BAD_FILTER_ACTION");
        governor.update(serviceMessage);
    }

    @Test
    public void drop() throws Exception {
        governor.drop(mailboxes.get(0).getId());
        assertNull(repository.findOne(mailboxes.get(0).getId()));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateName() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", ".qwer");
        governor.create(serviceMessage);
    }
}
