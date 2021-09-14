package ru.majordomo.hms.rc.user.test.managers;

import com.github.fppt.jedismock.RedisServer;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolationException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
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
public class GovernorOfDomainTest {
    @Autowired
    private GovernorOfDomain governor;

    @Autowired
    private DomainRepository repository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private PersonRepository personRepository;

    private static RedisServer redisServer;

    private List<Domain> domains;
    private List<Person> persons;

    @Before
    public void setUp() throws Exception {
        if (redisServer == null) {
            LettuceConnectionFactory redisConnectionFactory = (LettuceConnectionFactory) this.redisConnectionFactory;
            redisServer = new RedisServer(redisConnectionFactory.getPort());
            redisServer.start();
        }
        persons = ResourceGenerator.generateBatchOfPerson();
        domains = ResourceGenerator.generateBatchOfDomains(persons);
        repository.saveAll(domains);
        personRepository.saveAll(persons);
    }

    @After
    public void deleteAll() throws Exception {
        repository.deleteAll();
        personRepository.deleteAll();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        redisServer.stop();
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        Domain domain = governor.completeOversightAndStore(ovs);
        assertNotNull(domain);
        assertNotNull(domain.getRegSpec());
        assertThat(domain.getPersonId(), is(domains.get(0).getPersonId()));
    }

    @Test
    public void createWithoutRegister() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("register", false);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        Domain domain = governor.completeOversightAndStore(ovs);
        assertNotNull(domain);
        assertNull(domain.getPersonId());
        assertNull(domain.getRegSpec());
    }

    @Test(expected = ConstraintViolationException.class)
    public void creatingWithBadName() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "online");
        serviceMessage.addParam("register", false);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithRegisterWithoutPersonId() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("register", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithNameExists() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void setAutoRenew() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", domains.get(0).getId());
        serviceMessage.addParam("autoRenew", true);
        OperationOversight<Domain> ovs = governor.updateByOversight(serviceMessage);
        Domain domain = governor.completeOversightAndStore(ovs);
        assertThat(domain.getAutoRenew(), is(true));
    }

    @Test
    public void renew() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", domains.get(0).getId());
        serviceMessage.addParam("renew", true);
        RegSpec regSpec = domains.get(0).getRegSpec();
        regSpec.setPaidTill(regSpec.getPaidTill().plusYears(1));
        regSpec.setFreeDate(regSpec.getFreeDate().plusYears(1));
        OperationOversight<Domain> ovs = governor.updateByOversight(serviceMessage);
        Domain domain = governor.completeOversightAndStore(ovs);
        assertNotEquals(regSpec, domain.getRegSpec());
    }

    @Test
    public void buildByAccountIdAndName() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());
        Domain domain = governor.build(keyValue);
        assertNotNull(domain);
        assertThat(domain.getName(), is(domains.get(0).getName()));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithBadName() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "bad_domain_name");
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        governor.completeOversightAndStore(ovs);
    }

    @Test
    public void getExpiring() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("paidTillStart", "2017-09-01");
        keyValue.put("paidTillEnd", "2017-11-01");
        List<Domain> domains = (List<Domain>) governor.buildAll(keyValue);
        assertThat(domains.size(), is(2));
    }

    @Test
    public void getExpiringByAccount() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("paidTillStart", "2017-09-01");
        keyValue.put("paidTillEnd", "2017-11-01");
        keyValue.put("accountId", domains.get(0).getAccountId());
        List<Domain> domains = (List<Domain>) governor.buildAll(keyValue);
        System.out.println(domains);
        assertThat(domains.size(), is(1));
    }

    @Test
    public void buildWithName() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        Domain domain = governor.build(keyValue);
        System.out.println(domain.getName());
    }

    @Test
    public void createAndGenerateDkim() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("generateDkim", true);
        OperationOversight<Domain> ovs = governor.createByOversight(serviceMessage);
        Domain domain = governor.completeOversightAndStore(ovs);
        assertNotNull(domain.getDkim());
        assertFalse(StringUtils.isBlank(domain.getDkim().getPublicKey()));
        assertFalse(StringUtils.isBlank(domain.getDkim().getSelector()));
        String expectedData = "v=DKIM1; h=sha256; k=rsa; p=" + domain.getDkim().getPublicKey();
        assertEquals(expectedData, domain.getDkim().getData());
    }
}
