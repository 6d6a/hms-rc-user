package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.RedisConfig;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfMailbox;

import java.util.Arrays;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RedisConfig.class, ConfigGovernorOfMailbox.class, ConfigStaffResourceControllerClient.class}, webEnvironment = NONE)
public class GovernorOfMailboxTest {
    @Autowired
    private GovernorOfMailbox governor;
    @Autowired
    private MailboxRepository repository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;

    private List<Mailbox> mailboxes;
    private List<Domain> batchOfDomains;

    @Before
    public void setUp() throws Exception {
        batchOfDomains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain : batchOfDomains) {
            Person person = domain.getPerson();
            personRepository.save(person);
        }
        domainRepository.save(batchOfDomains);
        mailboxes = ResourceGenerator.generateBatchOfMailboxesWithDomains(batchOfDomains);

        repository.save(mailboxes);
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
//        redisRepository.deleteAll();
    }

    @Test
    public void redis() {
        governor.addToRedis(mailboxes.get(0));

//        redisRepository.save(mailboxes.get(0));
//        Mailbox mailbox = (Mailbox) governor.construct(redisRepository.findOne(mailboxes.get(0).getId()));
//        System.out.println();
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        governor.create(serviceMessage);
        Mailbox mailbox = repository.findByNameAndDomainId((String) serviceMessage.getParam("name"), batchOfDomains.get(0).getId());
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(0L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithDuplicateAddress() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("name");
        serviceMessage.addParam("name", mailboxes.get(0).getName());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutPassword() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.delParam("password");
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
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
    public void createWithBlacklistAndWhitelist() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.setAccountId(batchOfDomains.get(0).getAccountId());
        serviceMessage.addParam("blackList", Arrays.asList("ololo@bad.ru"));
        serviceMessage.addParam("whiteList", Arrays.asList("ololo@good.ru"));
        serviceMessage.addParam("redirectAddresses", Arrays.asList("ololo@redirect.ru"));
        governor.create(serviceMessage);
        Mailbox mailbox = repository.findByNameAndDomainId((String) serviceMessage.getParam("name"), batchOfDomains.get(0).getId());
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(0L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getWhiteList(), is(Arrays.asList("ololo@good.ru")));
        assertThat(mailbox.getBlackList(), is(Arrays.asList("ololo@bad.ru")));
        assertThat(mailbox.getRedirectAddresses(), is(Arrays.asList("ololo@redirect.ru")));
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(mailboxes.get(0).getDomainId());
        serviceMessage.delParam("name");
        serviceMessage.setAccountId(mailboxes.get(0).getAccountId());
        serviceMessage.addParam("resourceId", mailboxes.get(0).getId());
        serviceMessage.addParam("quota", 500000L);
        serviceMessage.addParam("blackList", Arrays.asList("ololo@bad.ru"));
        serviceMessage.addParam("whiteList", Arrays.asList("ololo@good.ru"));
        serviceMessage.addParam("redirectAddresses", Arrays.asList("ololo@redirect.ru"));
        String oldPasswordHash = mailboxes.get(0).getPasswordHash();
        governor.update(serviceMessage);
        Mailbox mailbox = repository.findOne(mailboxes.get(0).getId());
        assertNotNull(mailbox);
        assertNotNull(mailbox.getPasswordHash());
        assertThat(mailbox.getQuota(), is(500000L));
        assertThat(mailbox.getQuotaUsed(), is(0L));
        assertThat(mailbox.getWhiteList(), is(Arrays.asList("ololo@good.ru")));
        assertThat(mailbox.getBlackList(), is(Arrays.asList("ololo@bad.ru")));
        assertThat(mailbox.getRedirectAddresses(), is(Arrays.asList("ololo@redirect.ru")));
        assertThat(mailbox.getPasswordHash(), not(oldPasswordHash));
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

    @Test
    public void drop() throws Exception {
        governor.drop(mailboxes.get(0).getId());
        assertNull(repository.findOne(mailboxes.get(0).getId()));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }
}
