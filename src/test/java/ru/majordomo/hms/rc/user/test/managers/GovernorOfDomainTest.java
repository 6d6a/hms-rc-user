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
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDomain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigGovernorOfDomain.class,
                ConfigDomainRegistrarClient.class,
                ConfigStaffResourceControllerClient.class
        },
        webEnvironment = NONE
)
public class GovernorOfDomainTest {
    @Autowired
    private GovernorOfDomain governor;

    @Autowired
    private DomainRepository repository;

    @Autowired
    private PersonRepository personRepository;

    private List<Domain> domains;
    private List<Person> persons;

    @Before
    public void setUp() throws Exception {
        persons = ResourceGenerator.generateBatchOfPerson();
        domains = ResourceGenerator.generateBatchOfDomains(persons);
        repository.save(domains);
        personRepository.save(persons);
    }

    @After
    public void cleanUp() throws Exception {
        repository.deleteAll();
        personRepository.deleteAll();
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        Domain domain = (Domain) governor.create(serviceMessage);
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
        Domain domain = (Domain) governor.create(serviceMessage);
        assertNotNull(domain);
        assertNull(domain.getPersonId());
        assertNull(domain.getRegSpec());
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithRegisterWithoutPersonId() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "domain.com");
        serviceMessage.addParam("register", true);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutName() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithNameExists() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        governor.create(serviceMessage);
    }

    @Test
    public void setAutoRenew() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", domains.get(0).getId());
        serviceMessage.addParam("autoRenew", true);
        Domain domain = (Domain) governor.update(serviceMessage);
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
        Domain domain = (Domain) governor.update(serviceMessage);
        assertNotEquals(regSpec, domain.getRegSpec());
    }

    @Test
    public void buildByAccountIdAndName() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());
        Domain domain = (Domain) governor.build(keyValue);
        assertNotNull(domain);
        assertThat(domain.getName(), is(domains.get(0).getName()));
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadName() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "bad_domain_name");
        serviceMessage.addParam("personId", domains.get(0).getPersonId());
        serviceMessage.addParam("register", true);
        governor.create(serviceMessage);
    }
}
