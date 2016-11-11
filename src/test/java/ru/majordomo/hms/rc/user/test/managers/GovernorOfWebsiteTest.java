package ru.majordomo.hms.rc.user.test.managers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.ServiceType;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfWebsite;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigGovernorOfWebsite.class, webEnvironment = NONE, properties = {"default.service.name:WEBSITE_APACHE2_PHP56_DEFAULT"})
public class GovernorOfWebsiteTest {
    @Autowired
    private GovernorOfWebSite governor;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;;

    @Value("${default.service.name}")
    private String defaultServiceName;

    private List<String> domainIds = new ArrayList<>();
    private String accountId;

    @Before
    public void setUp() throws Exception {
        List<Domain> domains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain: domains) {
            Person person = domain.getPerson();
            personRepository.save(person);
            domainRepository.save(domain);
        }
        String domainId = domains.get(0).getId();
        domainIds.add(domainId);
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        for (UnixAccount unixAccount: unixAccounts) {
            unixAccountRepository.save(unixAccount);
        }
        accountId = unixAccounts.get(0).getAccountId();
    }

    @Test
    public void create() {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(domainIds, accountId);
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutDomains() {
        List<String> emptydomainIds = new ArrayList<>();
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(emptydomainIds, accountId);
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutAccountId() {
        String emptyString = "";
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(domainIds, emptyString);
        System.out.println(serviceMessage.toString());
        governor.create(serviceMessage);
    }

    @After
    public void deleteAll() {
        domainRepository.deleteAll();
        personRepository.deleteAll();
        unixAccountRepository.deleteAll();
    }
}
