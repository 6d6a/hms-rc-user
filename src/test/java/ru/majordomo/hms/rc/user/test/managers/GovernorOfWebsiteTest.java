package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.CharSet;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;
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
import static org.hamcrest.CoreMatchers.is;

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
public class GovernorOfWebsiteTest {
    @Autowired
    private GovernorOfWebSite governor;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;
    @Autowired
    private WebSiteRepository webSiteRepository;
    @Autowired
    private StaffResourceControllerClient staffResourceControllerClient;

    private List<String> domainIds = new ArrayList<>();
    private String accountId;
    private List<WebSite> batchOfWebsites;
    private String serviceId;

    @Before
    public void setUp() throws Exception {
        List<Domain> domains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain: domains) {
            Person person = domain.getPerson();
            personRepository.save(person);
            domainRepository.save(domain);
            domainIds.add(domain.getId());
        }
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        unixAccountRepository.saveAll(unixAccounts);
        accountId = unixAccounts.get(0).getAccountId();

        batchOfWebsites = new ArrayList<>();

        serviceId = staffResourceControllerClient.getActiveHostingServer().getServiceIds().get(0);

        batchOfWebsites = ResourceGenerator.generateBatchOfCertainWebsites(accountId, serviceId, unixAccounts.get(0).getId(), domainIds);

        webSiteRepository.saveAll(batchOfWebsites);
    }

    @Test
    public void create() {
        List<Domain> domains = ResourceGenerator.generateBatchOfDomains();
        List<String> localDomainIds = new ArrayList<>();
        for (Domain domain: domains) {
            Person p = domain.getPerson();
            personRepository.save(p);
            domainRepository.save(domain);
            localDomainIds.add(domain.getId());
        }

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(localDomainIds, accountId);
        WebSite webSite = governor.create(serviceMessage);
        assertThat(webSite.getStaticFileExtensions().size(), is(18));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutDomains() {
        List<String> emptydomainIds = new ArrayList<>();
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(emptydomainIds, accountId);

        governor.create(serviceMessage);
    }

    @Test(expected = ParameterValidationException.class)
    public void createWithoutAccountId() {
        String emptyString = "";
        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteCreateServiceMessage(domainIds, emptyString);

        governor.create(serviceMessage);
    }

    @Test
    public void update1() {
        List<String> domainIdsLocal = batchOfWebsites.get(0).getDomainIds();
        String accountIdLocal = batchOfWebsites.get(0).getAccountId();
        List<String> cgiExtensions = new ArrayList<>();
        cgiExtensions.add("py");
        cgiExtensions.add("log");

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteUpdateServiceMessage(domainIdsLocal, accountIdLocal);
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("applicationServiceId", serviceId);

        WebSite webSite = governor.update(serviceMessage);
        System.out.println(webSite.getCgiFileExtensions());

        WebSite foundWebSite = webSiteRepository.findById(batchOfWebsites.get(0).getId()).orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        Assert.isTrue(foundWebSite.getCgiEnabled(), "CgiEnabled must be true");
        Assert.isTrue(foundWebSite.getCgiFileExtensions().equals(cgiExtensions), "CgiFileExtensions.equals(cgiExtensions) must be true");
        Assert.isTrue(foundWebSite.getMbstringFuncOverload() == 4, "MbstringFuncOverload==4 must be true");
        Assert.isTrue(foundWebSite.getAllowUrlFopen(), "AllowUrlFopen must be true");
        Assert.isTrue(foundWebSite.getMultiViews(), "MultiViews must be true");
        Assert.isTrue(!foundWebSite.getFollowSymLinks(), "!FollowSymLinks must be true");
        Assert.isTrue(!foundWebSite.getAccessLogEnabled(), "!AccessLogEnabled must be true");
        Assert.isTrue(foundWebSite.getAutoSubDomain(), "AutoSubDomain must be true");
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateNonExistentDomainIds() {
        List<String> domainIdsLocal = Collections.singletonList(ObjectId.get().toString());
        String accountIdLocal = batchOfWebsites.get(0).getAccountId();

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteUpdateServiceMessage(domainIdsLocal, accountIdLocal);
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("applicationServiceId", serviceId);

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void updateNonExistentServiceId() {
        List<String> domainIdsLocal = batchOfWebsites.get(0).getDomainIds();
        String accountIdLocal = batchOfWebsites.get(0).getAccountId();

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteUpdateServiceMessage(domainIdsLocal, accountIdLocal);
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("applicationServiceId", ObjectId.get().toString());

        governor.update(serviceMessage);
    }

    @Test(expected = ParameterValidationException.class)
    public void updateBadParameter() {
        List<String> domainIdsLocal = batchOfWebsites.get(0).getDomainIds();
        String accountIdLocal = batchOfWebsites.get(0).getAccountId();
        List<String> cgiExtensions = new ArrayList<>();
        cgiExtensions.add("py");
        cgiExtensions.add("lol");

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteUpdateServiceMessage(domainIdsLocal, accountIdLocal);
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("applicationServiceId", serviceId);
        serviceMessage.addParam("mbstringFuncOverload", "Валера");

        governor.update(serviceMessage);

        WebSite foundWebSite = webSiteRepository.findById(batchOfWebsites.get(0).getId()).orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));

        Assert.isTrue(foundWebSite.getCgiEnabled(), "CgiEnabled must be true");
        Assert.isTrue(foundWebSite.getCgiFileExtensions().equals(cgiExtensions), "CgiFileExtensions.equals(cgiExtensions) must be true");
    }

    @Test
    public void drop() throws Exception {
        String resourceId = batchOfWebsites.get(0).getId();

        governor.drop(resourceId);

        Assert.isTrue(webSiteRepository.count() == 1, "webSiteRepository.count() == 1 must be true");
        Assert.isNull(webSiteRepository.findById(resourceId).orElse(null), "webSiteRepository.findOne(resourceId) must be null");
    }

    @Test(expected = ResourceNotFoundException.class)
    public void dropNonExistent() throws Exception {
        governor.drop(ObjectId.get().toString());
    }

    @Test
    public void buildByDomainId() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", batchOfWebsites.get(0).getAccountId());
        keyValue.put("domainId", batchOfWebsites.get(0).getDomainIds().get(0));
        WebSite webSite = governor.build(keyValue);
        System.out.println(webSite);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateScriptAlias() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("scriptAlias", "В%алера");

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateIndexFileList() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("indexFileList", Arrays.asList(".index.php", "index2.php"));

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateCgiFileExtensions() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("cgiFileExtensions", Arrays.asList("py", ".log"));

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateSsiFileExtensions() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("ssiFileExtensions", Arrays.asList("htm", "fer4#"));

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateExpires() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("expires", "lol");

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateDocumentRoot() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("documentRoot", "/home/u100800");

        governor.update(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void validateDocumentRoot2() throws Exception {
        ServiceMessage serviceMessage = prepareWebsiteUpdateServiceMessage();
        serviceMessage.addParam("documentRoot", "asdasdasdasd.com/www/../../../123");

        governor.update(serviceMessage);
    }

    @After
    public void deleteAll() {
        domainRepository.deleteAll();
        personRepository.deleteAll();
        unixAccountRepository.deleteAll();
        webSiteRepository.deleteAll();
    }

    private ServiceMessage prepareWebsiteUpdateServiceMessage() throws Exception {
        List<String> domainIdsLocal = batchOfWebsites.get(0).getDomainIds();
        String accountIdLocal = batchOfWebsites.get(0).getAccountId();

        ServiceMessage serviceMessage = ServiceMessageGenerator.generateWebsiteUpdateServiceMessage(domainIdsLocal, accountIdLocal);
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());

        return serviceMessage;
    }
}
