package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfMailbox;

import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigGovernorOfMailbox.class, webEnvironment = NONE)
public class GovernorOfMailboxTest {
    @Autowired
    private GovernorOfMailbox governor;
    @Autowired
    private MailboxRepository repository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;


    private List<Domain> batchOfDomains;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
        batchOfDomains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain: batchOfDomains) {
            personRepository.save(domain.getPerson());
            domainRepository.save(domain);
        }
    }

    @Test
    public void create() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadQuota() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.addParam("quota", -1L);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithBadQuotaUsed() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.addParam("quotaUsed", -2L);
        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidateException.class)
    public void createWithoutDomain() throws Exception {
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateMailboxCreateServiceMessage(batchOfDomains.get(0).getId());
        serviceMessage.delParam("domainId");
        governor.create(serviceMessage);
    }

}
