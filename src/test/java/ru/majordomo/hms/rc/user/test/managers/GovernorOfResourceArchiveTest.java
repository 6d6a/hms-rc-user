package ru.majordomo.hms.rc.user.test.managers;

import org.apache.commons.lang.NotImplementedException;
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
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
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

                ConfigGovernors.class
        },
        webEnvironment = NONE,
        properties = {
                "default.website.serviceName=WEBSITE_APACHE2_PHP56_DEFAULT",
                "default.website.documentRootPattern=/www",
                "default.website.charset=UTF8",
                "default.website.ssi.enabled=true",
                "default.website.ssi.fileExtensions=shtml,shtm",
                "default.website.cgi.enabled=false",
                "default.website.cgi.fileExtensions=cgi,pl",
                "default.website.scriptAlias=cgi-bin",
                "default.website.ddosProtection=true",
                "default.website.autoSubDomain=false",
                "default.website.accessByOldHttpVersion=false",
                "default.website.static.fileExtensions=avi,bz2,css,gif,gz,jpg,jpeg,js,mp3,mpeg,ogg,png,rar,svg,swf,zip,html,htm",
                "default.website.indexFileList=index.php,index.html,index.htm",
                "default.website.customUserConf=",
                "default.website.accessLogEnabled=true",
                "default.website.errorLogEnabled=true",
                "default.website.allowUrlFopen=false",
                "default.website.mbstringFuncOverload=0",
                "default.website.followSymLinks=true",
                "default.website.multiViews=false",
                "default.archive.hostname=archive.majordomo.ru",
                "resources.quotable.warnPercent.mailbox=90"
        }
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

    @Value("${default.website.serviceName}")
    private String defaultServiceName;

    @Value("${default.website.documentRootPattern}")
    private String defaultWebsiteDocumetRootPattern;

    @Value("${default.website.charset}")
    private CharSet defaultWebsiteCharset;

    @Value("${default.website.ssi.enabled}")
    private Boolean defaultWebsiteSsiEnabled;

    @Value("${default.website.ssi.fileExtensions}")
    private List<String> defaultWebsiteSsiFileExtensions;

    @Value("${default.website.cgi.enabled}")
    private Boolean defaultWebsiteCgiEnabled;

    @Value("${default.website.cgi.fileExtensions}")
    private List<String> defaultWebsiteCgiFileExtensions;

    @Value("${default.website.scriptAlias}")
    private String defaultWebsiteScriptAliace;

    @Value("${default.website.ddosProtection}")
    private Boolean defaultWebsiteDdosProtection;

    @Value("${default.website.autoSubDomain}")
    private Boolean defaultWebsiteAutoSubDomain;

    @Value("${default.website.accessByOldHttpVersion}")
    private Boolean defaultWebsiteAccessByOldHttpVersion;

    @Value("${default.website.static.fileExtensions}")
    private List<String> defaultWebsiteStaticFileExtensions;

    @Value("${default.website.indexFileList}")
    private List<String> defaultWebsiteIndexFileList;

    @Value("${default.website.customUserConf}")
    private String defaultWebsiteCustomUserConf;

    @Value("${default.website.accessLogEnabled}")
    private Boolean defaultAccessLogEnabled;

    @Value("${default.website.errorLogEnabled}")
    private Boolean defaultErrorLogEnabled;

    @Value("${default.website.allowUrlFopen}")
    private Boolean defaultAllowUrlFopen;

    @Value("${default.website.mbstringFuncOverload}")
    private Boolean defaultMbstringFuncOverload;

    @Value("${default.website.followSymLinks}")
    private Boolean defaultFollowSymLinks;

    @Value("${default.website.multiViews}")
    private Boolean defaultMultiViews;

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
        unixAccountRepository.save(unixAccounts);
        accountId = unixAccounts.get(0).getAccountId();

        batchOfWebsites = new ArrayList<>();

        serviceId = staffResourceControllerClient.getActiveHostingServer().getServiceIds().get(0);

        batchOfWebsites = ResourceGenerator.generateBatchOfCertainWebsites(accountId, serviceId, unixAccounts.get(0).getId(), domainIds);

        webSiteRepository.save(batchOfWebsites);

        batchOfDatabases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database: batchOfDatabases) {
            databaseUserRepository.save(database.getDatabaseUsers());
            databaseRepository.save(database);
        }
    }

    @Test
    public void createWebsiteArchive() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(batchOfWebsites.get(0).getAccountId());
        serviceMessage.addParam("resourceId", batchOfWebsites.get(0).getId());
        serviceMessage.addParam("resourceType", "WEBSITE");
        ResourceArchive createdArchive = governor.create(serviceMessage);

        ResourceArchive archive = governor.build(createdArchive.getId());
        assertNotNull(archive.getFileLink());
        assertNotNull(archive.getResource());
        assertThat(archive.getResourceType(), is(ResourceArchiveType.WEBSITE));
        assertThat(archive.getServiceId(), is(batchOfWebsites.get(0).getServiceId()));
    }

    @Test
    public void createDatabaseArchive() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setAccountId(batchOfDatabases.get(0).getAccountId());
        serviceMessage.addParam("resourceId", batchOfDatabases.get(0).getId());
        serviceMessage.addParam("resourceType", "DATABASE");
        ResourceArchive createdArchive = governor.create(serviceMessage);

        ResourceArchive archive = governor.build(createdArchive.getId());
        assertNotNull(archive.getFileLink());
        assertNotNull(archive.getResource());
        assertThat(archive.getResourceType(), is(ResourceArchiveType.DATABASE));
        assertThat(archive.getServiceId(), is(batchOfDatabases.get(0).getServiceId()));
    }

    @Test(expected = NotImplementedException.class)
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        governor.update(serviceMessage);
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
