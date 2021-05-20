package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.test.context.junit4.SpringRunner;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringRunner.class)
@EnableMongoAuditing
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class,

                DefaultWebSiteSettings.class
        },
        webEnvironment = NONE
)
public class GovernorOfResourceArchiveTest {
    @Autowired
    private GovernorOfResourceArchive governor;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;
    @Autowired
    private WebSiteRepository webSiteRepository;
    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private DatabaseUserRepository databaseUserRepository;
    @Autowired
    private ResourceArchiveRepository repository;
    @Autowired
    StaffResourceControllerClient staffResourceControllerClient;

    @Value("${default.archive.hostname}")
    private String archiveHostname;

    private List<String> domainIds = new ArrayList<>();
    private String accountId;
    private List<WebSite> batchOfWebsites;
    private List<Database> batchOfDatabases;
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

        serviceId = staffResourceControllerClient.getActiveHostingServer(false).getServiceIds().get(0);

        batchOfWebsites = ResourceGenerator.generateBatchOfCertainWebsites(accountId, serviceId, unixAccounts.get(0).getId(), domainIds);

        webSiteRepository.saveAll(batchOfWebsites);

        batchOfDatabases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database: batchOfDatabases) {
            databaseUserRepository.saveAll(database.getDatabaseUsers());
            databaseRepository.save(database);
        }
    }

    @Test
    public void createWebsiteArchive() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(batchOfWebsites.get(0).getAccountId());
        serviceMessage.addParam("archivedResourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("resourceType", "WEBSITE");
        OperationOversight<ResourceArchive> ovs = governor.createByOversight(serviceMessage);
        ResourceArchive createdArchive = governor.completeOversightAndStore(ovs);

        ResourceArchive archive = governor.build(createdArchive.getId());
        assertNotNull(archive.getFileLink());
        assertNotNull(archive.getResource());
        assertThat(archive.getResourceType(), is(ResourceArchiveType.WEBSITE));
        assertThat(archive.getServiceId(), is(batchOfWebsites.get(0).getServiceId()));
        assertThat(archive.getArchivedResourceId(), is(batchOfWebsites.get(0).getId()));
    }

    @Test
    public void createDatabaseArchive() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(batchOfDatabases.get(0).getAccountId());
        serviceMessage.addParam("archivedResourceId", batchOfDatabases.get(0).getId());
        serviceMessage.addParam("resourceType", "DATABASE");
        OperationOversight<ResourceArchive> ovs = governor.createByOversight(serviceMessage);
        ResourceArchive createdArchive = governor.completeOversightAndStore(ovs);

        ResourceArchive archive = governor.build(createdArchive.getId());
        assertNotNull(archive.getFileLink());
        assertNotNull(archive.getResource());
        assertThat(archive.getResourceType(), is(ResourceArchiveType.DATABASE));
        assertThat(archive.getServiceId(), is(batchOfDatabases.get(0).getServiceId()));
        assertThat(archive.getArchivedResourceId(), is(batchOfDatabases.get(0).getId()));
    }

    @After
    public void deleteAll() {
        domainRepository.deleteAll();
        personRepository.deleteAll();
        unixAccountRepository.deleteAll();
        webSiteRepository.deleteAll();
        databaseRepository.deleteAll();
        databaseUserRepository.deleteAll();
    }
}
