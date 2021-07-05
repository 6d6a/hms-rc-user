package ru.majordomo.hms.rc.user.test.managers;

import com.github.fppt.jedismock.RedisServer;
import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.repositoriesRedis.MailboxRedisRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
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

import java.net.IDN;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolationException;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static ru.majordomo.hms.rc.user.common.Constants.IS_AGGREGATOR_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.RESOURCE_ID_KEY;

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

    private final static String SERVER_NAME = "pop100500";

    @Before
    public void setUp() throws Exception {
        if (redisServer == null) {
            LettuceConnectionFactory redisConnectionFactory = (LettuceConnectionFactory) this.redisConnectionFactory;
            redisServer = new RedisServer(redisConnectionFactory.getPort());
            redisServer.start();
        }
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        batchOfDomains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain : batchOfDomains) {
            Person person = domain.getPerson();
            personRepository.save(person);
        }
        domainRepository.saveAll(batchOfDomains);

        unixAccounts.get(0).setAccountId(batchOfDomains.get(0).getAccountId());
        unixAccountRepository.saveAll(unixAccounts);

        mailboxes = ResourceGenerator.generateBatchOfMailboxesWithDomains(batchOfDomains);

        for (Mailbox mailbox : mailboxes) {
            mailbox.setUid(unixAccounts.get(0).getUid());
            mailbox.setMailSpool("/homebig/" + mailbox.getDomain().getName());
            governor.syncWithRedis(mailbox);
        }

        repository.saveAll(mailboxes);
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
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Mailbox mailbox = repository.findByNameAndDomainId((String) serviceMessage.getParam("name"), batchOfDomains.get(0).getId());

        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(250000L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getMailFromAllowed(), is(true));
        assertThat(mailbox.getAntiSpamEnabled(), is(false));

        MailboxForRedis redisMailbox = redisRepository
                .findById(governor.construct(mailbox).getFullName())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
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
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutPassword() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("password");
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadQuota() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("quota", -1L);
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithoutDomain() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("domainId");
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
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
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

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

    @Test(expected = ParameterValidationException.class)
    public void createWithBadSpamFilterMood() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("spamFilterMood", "BAD_FILTER_MOOD");
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithBadSpamFilterAction() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("spamFilterAction", "BAD_FILTER_ACTION");
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
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
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Mailbox mailbox = repository
                .findById(mailboxes.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(500000L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getWhiteList(), is(Arrays.asList("good.ru", "ololo@my-friend.com")));
        assertThat(mailbox.getBlackList(), is(Arrays.asList("ololo@bad.ru", "spam.com")));
        assertThat(mailbox.getRedirectAddresses(), is(Collections.singletonList("ololo@redirect.ru")));
        assertThat(mailbox.getPasswordHash(), not(mailboxes.get(0).getPasswordHash()));

        MailboxForRedis redisMailbox = redisRepository
                .findById(governor.construct(mailbox).getFullName())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
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
    public void updateSetAggregatorAndDropAggregator() throws Exception {
        Mailbox workedMailbox = mailboxes.get(0);
        governor.construct(workedMailbox);
        final String workedMailboxId = workedMailbox.getId();
        final String workedMailboxName = workedMailbox.getName();
        final String workedMailboxDomainName = workedMailbox.getDomain().getName();
        final String workedMailboxFullName = workedMailbox.getFullNameInPunycode();
        final String workedMailboxAggregatorId = MailboxForRedis.getAggregatorRedisId(workedMailboxDomainName);

        Assert.assertNotNull(workedMailboxFullName);
        Assert.assertFalse(redisRepository.findById(workedMailboxAggregatorId).isPresent());
        Assert.assertFalse(repository.existsByDomainIdAndIsAggregator(workedMailbox.getDomainId(), true));

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(workedMailbox.getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(workedMailbox.getAccountId());
        serviceMessage.addParam(RESOURCE_ID_KEY, workedMailbox.getId());
        serviceMessage.addParam(IS_AGGREGATOR_KEY, true);
        // end prepare

        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        Mailbox mailbox = repository.findById(workedMailboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertNotNull(mailbox.getIsAggregator());
        assertTrue(mailbox.getIsAggregator());
        assertEquals(workedMailboxName, mailbox.getName());

        MailboxForRedis redisMailbox = redisRepository.findById(workedMailboxAggregatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertNotNull(redisMailbox.getId());
        assertEquals(workedMailboxFullName, redisMailbox.getRedirectAddresses());
        assertEquals(SERVER_NAME, redisMailbox.getServerName());
        assertTrue(redisMailbox.getWritable());
        assertTrue(redisRepository.isAggregator(mailbox.getName(), workedMailboxDomainName));

        // second part. Not change aggregator
        serviceMessage.delParam(IS_AGGREGATOR_KEY);
        serviceMessage.addParam(IS_AGGREGATOR_KEY, null);
        ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        mailbox = repository.findById(workedMailboxId).get();
        assertNotNull(mailbox.getIsAggregator());
        assertTrue(mailbox.getIsAggregator());

        redisMailbox = redisRepository.findById(workedMailboxAggregatorId).get();
        assertNotNull(redisMailbox.getId());
        assertEquals(workedMailboxFullName, redisMailbox.getRedirectAddresses());
        assertEquals(SERVER_NAME, redisMailbox.getServerName());
        assertTrue(redisMailbox.getWritable());
        assertTrue(redisRepository.isAggregator(mailbox.getName(), workedMailboxDomainName));

        // third part disable aggregator
        serviceMessage.delParam(IS_AGGREGATOR_KEY);
        serviceMessage.addParam(IS_AGGREGATOR_KEY, false);
        ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        mailbox = repository.findById(workedMailboxId).get();
        assertNotEquals(Boolean.TRUE, mailbox.getIsAggregator());

        assertFalse(repository.existsByDomainIdAndIsAggregator(workedMailbox.getDomainId(), true));
        assertFalse(redisRepository.findById(workedMailboxAggregatorId).isPresent());

        // fourth part. return aggregator
        serviceMessage.delParam(IS_AGGREGATOR_KEY);
        serviceMessage.addParam(IS_AGGREGATOR_KEY, true);
        ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

        mailbox = repository.findById(workedMailboxId).get();
        assertNotNull(mailbox.getIsAggregator());
        assertTrue(mailbox.getIsAggregator());
        assertEquals(workedMailboxName, mailbox.getName());

        redisMailbox = redisRepository.findById(workedMailboxAggregatorId).get();
        assertNotNull(redisMailbox.getId());
        assertEquals(workedMailboxFullName, redisMailbox.getRedirectAddresses());
        assertEquals(SERVER_NAME, redisMailbox.getServerName());
        assertTrue(redisMailbox.getWritable());
        assertTrue(redisRepository.isAggregator(mailbox.getName(), workedMailboxDomainName));

        // fifth part delete mailbox with aggregator
        ovs = governor.dropByOversight(workedMailboxId);
        governor.completeOversightAndDelete(ovs);
        Assert.assertFalse(redisRepository.isAggregator(workedMailboxName, workedMailboxDomainName));
        Assert.assertFalse(redisRepository.existsById(workedMailboxAggregatorId));
    }

    @Test
    public void updateSetMailFromAllowed() throws Exception {
        ServiceMessage serviceMessage= ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("mailFromAllowed", false);
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
        Mailbox mailbox = repository
                .findById(mailboxes.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertThat(mailbox.getMailFromAllowed(), is(false));
        MailboxForRedis redisMailbox = redisRepository
                .findById(governor.construct(mailbox).getFullName())
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        assertNotNull(redisMailbox);
        assertThat(mailbox.getMailFromAllowed(), is(redisMailbox.getMailFromAllowed()));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateWithoutResourceId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("quota", 300000L);
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateWithoutAccountId() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("quota", 300000L);
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void updateWithBadFilterMood() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("spamFilterMood", "BAD_FILTER_MOOD");
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void updateWithBadFilterAction() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("spamFilterAction", "BAD_FILTER_ACTION");
        OperationOversight<Mailbox> ovs = governor.updateByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void drop() throws Exception {
        governor.drop(mailboxes.get(0).getId());
        assertNull(repository.findById(mailboxes.get(0).getId()).orElse(null));
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
        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void validateAllowedIps() {
        Arrays.asList(
                " sdg",
                "15135",
                "1.1.1.1",
                "111.1.1.1",
                "111.1.1.1/0",
                "111.1.1.1/45",
                "111.1.1.1/01",
                "111.1.1.1/-1",
                "1111.1.1.1/14",
                "331.1.1.1/14",
                "256.1.1.1/14",
                "6.256.1.1/14",
                "6.265.1.1/14",
                "6.26.265.1/14",
                "6.26.2.335/14"
        ).forEach(ip -> {
            ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
            serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
            serviceMessage.addParam("allowedIps", Arrays.asList(ip));

            try {
                OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
                governor.completeOversightAndStore(ovs);
                throw new RuntimeException("ip " + ip + " прошел проверку cidr");
            } catch (ConstraintViolationException e) {} //its ok
        });
    }

    @Test
    public void validateValidAllowedIps() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("allowedIps", Arrays.asList("84.240.40.0/24", "111.1.1.1/32"));

        OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);

    }

    @Test
    public void invalidLocalNameTest() {
        Arrays.asList(
                ".sdg",
                "/.dsg",
                "/.d!@#$%^&*()sg",
                "kasdgjjsdgkjasg/",
                "/kasdgjjsdgkjasg",
                "asdgjjsd./gkjasg",
                "asdgjjsd/.gkjasg",
                "asdgjjsd.",
                "asdgjjsd/gkjasg",
                "/",
                "русский",
                "uPpEr",
                "*"
        ).forEach(localName -> {
            ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
            serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
            serviceMessage.addParam("name", localName);

            try {
                OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
                governor.completeOversightAndStore(ovs);
                throw new RuntimeException("localName " + localName + " прошел проверку");
            } catch (ConstraintViolationException e) {} //its ok
        });
    }

    @Test
    public void validLocalNameTest() {
        Arrays.asList(
//                "asdgasdg?2452.dgj",
//                "asdgasdg!#$%&'*+=?^_`{|}~-2452.dgj",
//                "asdgasdg2!452.dgj",
                "asdgasdg2452.dgj",
                "asdgasdg2452-dgj",
                "asdgasdg2452_dgj"
        ).forEach(localName -> {
            ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
            serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
            serviceMessage.addParam("name", localName);
            OperationOversight<Mailbox> ovs = governor.createByOversight(serviceMessage);
            governor.completeOversightAndStore(ovs);
        });
    }

    @Test
    public void testUpdateAggregatorInRedis() {
        Domain ruDomain = batchOfDomains.stream()
                .filter(d -> Pattern.compile(".*[а-яА-Я].*").matcher(d.getName()).matches())
                .findFirst().get();

        Mailbox mailbox = mailboxes.stream().filter(m -> m.getDomainId().equals(ruDomain.getId())).findFirst().get();
        mailbox.setDomain(ruDomain);
        String mailboxFullName = mailbox.getName() + "@" + IDN.toASCII(ruDomain.getName());
        String aggregatorRedisId = MailboxForRedis.getAggregatorRedisId(ruDomain.getName());
        Assert.assertFalse(redisRepository.existsById(aggregatorRedisId));
        //end prepare

        mailbox.setIsAggregator(true);
        boolean isChanged = governor.updateAggregatorInRedis(mailbox, SERVER_NAME);
        Assert.assertTrue(isChanged);
        MailboxForRedis aggregatorRedis = redisRepository.findById(aggregatorRedisId).get();
        Assert.assertNotNull(aggregatorRedis.getRedirectAddresses());
        Assert.assertEquals(mailboxFullName, aggregatorRedis.getRedirectAddresses());

        mailbox.setIsAggregator(null);
        isChanged = governor.updateAggregatorInRedis(mailbox, SERVER_NAME);
        Assert.assertFalse(isChanged);
        Assert.assertTrue(redisRepository.existsById(aggregatorRedisId));

        mailbox.setIsAggregator(false);
        isChanged = governor.updateAggregatorInRedis(mailbox, SERVER_NAME);
        Assert.assertTrue(isChanged);
        Assert.assertFalse(redisRepository.existsById(aggregatorRedisId));

        isChanged = governor.updateAggregatorInRedis(mailbox, SERVER_NAME);
        Assert.assertFalse(isChanged);
        Assert.assertFalse(redisRepository.existsById(aggregatorRedisId));

        mailbox.setIsAggregator(null);
        isChanged = governor.updateAggregatorInRedis(mailbox, SERVER_NAME);
        Assert.assertFalse(isChanged);
        Assert.assertFalse(redisRepository.existsById(aggregatorRedisId));
    }

    @Test
    public void testUnmarkOtherAggregatorInMongo() {
        Domain ruDomain = batchOfDomains.stream()
                .filter(d -> Pattern.compile(".*[а-яА-Я].*").matcher(d.getName()).matches())
                .findFirst().get();
        List<Mailbox> testedMailboxes = repository.findByDomainId(ruDomain.getId());
        Mailbox initialMailbox = testedMailboxes.get(0);
        String initialMailboxId = initialMailbox.getId();
        Assert.assertNotNull(initialMailbox);
        initialMailbox.setDomain(ruDomain);
        for(int i = 0; i < 2; i++) {
            initialMailbox.setId(null);
            initialMailbox.setName("unmarkotheraggregator" + i);
            repository.insert(initialMailbox);
        }
        testedMailboxes = repository.findByDomainId(ruDomain.getId());
        Assert.assertTrue(testedMailboxes.size() >= 3);
        Assert.assertFalse(repository.existsByDomainIdAndIsAggregator(ruDomain.getId(), true));
        int mailboxCount = testedMailboxes.size();
        //end prepare


        for (Mailbox mailbox : testedMailboxes) {
            mailbox.setIsAggregator(true);
            repository.save(mailbox);
        }
        initialMailbox = repository.findById(initialMailboxId).get();
        Assert.assertTrue(repository.existsByDomainIdAndIsAggregator(ruDomain.getId(), true));

        long modified = governor.unmarkOtherAggregatorInMongo(initialMailbox);
        testedMailboxes = repository.findByDomainId(ruDomain.getId());
        Assert.assertEquals(modified + 1, testedMailboxes.size());
        Assert.assertTrue(testedMailboxes.stream().allMatch(mailbox -> (initialMailboxId.equals(mailbox.getId()) == Boolean.TRUE.equals(mailbox.getIsAggregator()))));


        for (Mailbox mailbox : testedMailboxes) {
            mailbox.setIsAggregator(true);
            repository.save(mailbox);
        }
        initialMailbox = repository.findById(initialMailboxId).get();
        Assert.assertTrue(repository.existsByDomainIdAndIsAggregator(ruDomain.getId(), true));
        Mailbox notExistsMailbox = repository.findById(initialMailboxId).get();
        notExistsMailbox.setId(new ObjectId().toString());
        initialMailbox.setName("unmarkotheraggregator-not-exists");
        initialMailbox.setIsAggregator(true);

        modified = governor.unmarkOtherAggregatorInMongo(notExistsMailbox);
        testedMailboxes = repository.findByDomainId(ruDomain.getId());
        Assert.assertEquals(modified, testedMailboxes.size());
        Assert.assertTrue(testedMailboxes.stream().noneMatch(mailbox -> Boolean.TRUE.equals(mailbox.getIsAggregator())));

    }
}
