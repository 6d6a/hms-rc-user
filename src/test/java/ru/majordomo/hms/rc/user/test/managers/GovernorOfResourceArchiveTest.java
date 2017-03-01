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
import org.springframework.util.Assert;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.common.ServiceMessageGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDatabase;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfWebsite;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.ArrayList;
import java.util.Arrays;
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

                ConfigGovernors.class
        },
        webEnvironment = NONE,
        properties = {
                "default.website.service.name:WEBSITE_APACHE2_PHP56_DEFAULT",
                "default.website.documet.root.pattern:/www",
                "default.website.charset:UTF8",
                "default.website.ssi.enabled:true",
                "default.website.ssi.file.extensions:shtml,shtm",
                "default.website.cgi.enabled:false",
                "default.website.cgi.file.extensions:cgi,pl",
                "default.website.script.aliace:cgi-bin",
                "default.website.ddos.protection:true",
                "default.website.auto.sub.domain:false",
                "default.website.access.by.old.http.version:false",
                "default.website.static.file.extensions:avi,bz2,css,gif,gz,jpg,jpeg,js,mp3,mpeg,ogg,png,rar,svg,swf,zip,html,htm",
                "default.website.index.file.list:index.php,index.html,index.htm",
                "default.website.custom.user.conf:",
                "default.website.access.log.enabled:true",
                "default.website.error.log.enabled:true",
                "default.website.allow.url.fopen:false",
                "default.website.mbstring.func.overload:0",
                "default.website.follow.sym.links:true",
                "default.website.multi.views:false",
                "default.archive.hostname:archive.majordomo.ru"
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

    @Value("${default.website.service.name}")
    private String defaultServiceName;

    @Value("${default.website.documet.root.pattern}")
    private String defaultWebsiteDocumetRootPattern;

    @Value("${default.website.charset}")
    private CharSet defaultWebsiteCharset;

    @Value("${default.website.ssi.enabled}")
    private Boolean defaultWebsiteSsiEnabled;

    @Value("${default.website.ssi.file.extensions}")
    private List<String> defaultWebsiteSsiFileExtensions;

    @Value("${default.website.cgi.enabled}")
    private Boolean defaultWebsiteCgiEnabled;

    @Value("${default.website.cgi.file.extensions}")
    private List<String> defaultWebsiteCgiFileExtensions;

    @Value("${default.website.script.aliace}")
    private String defaultWebsiteScriptAliace;

    @Value("${default.website.ddos.protection}")
    private Boolean defaultWebsiteDdosProtection;

    @Value("${default.website.auto.sub.domain}")
    private Boolean defaultWebsiteAutoSubDomain;

    @Value("${default.website.access.by.old.http.version}")
    private Boolean defaultWebsiteAccessByOldHttpVersion;

    @Value("${default.website.static.file.extensions}")
    private List<String> defaultWebsiteStaticFileExtensions;

    @Value("${default.website.index.file.list}")
    private List<String> defaultWebsiteIndexFileList;

    @Value("${default.website.custom.user.conf}")
    private String defaultWebsiteCustomUserConf;

    @Value("${default.website.access.log.enabled}")
    private Boolean defaultAccessLogEnabled;

    @Value("${default.website.error.log.enabled}")
    private Boolean defaultErrorLogEnabled;

    @Value("${default.website.allow.url.fopen}")
    private Boolean defaultAllowUrlFopen;

    @Value("${default.website.mbstring.func.overload}")
    private Boolean defaultMbstringFuncOverload;

    @Value("${default.website.follow.sym.links}")
    private Boolean defaultFollowSymLinks;

    @Value("${default.website.multi.views}")
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
        }
        String domainId = domains.get(0).getId();
        domainIds.add(domainId);
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        for (UnixAccount unixAccount: unixAccounts) {
            unixAccountRepository.save(unixAccount);
        }
        accountId = unixAccounts.get(0).getAccountId();

        batchOfWebsites = new ArrayList<>();

        serviceId = staffResourceControllerClient.getActiveHostingServer().getServiceIds().get(0);

        batchOfWebsites = ResourceGenerator.generateBatchOfCertainWebsites(accountId, serviceId, unixAccounts.get(0).getId(), domainIds);

        webSiteRepository.save(batchOfWebsites);

        batchOfDatabases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database: batchOfDatabases) {
            for (DatabaseUser databaseUser : database.getDatabaseUsers()) {
                databaseUserRepository.save(databaseUser);
            }
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
        ResourceArchive createdArchive = (ResourceArchive) governor.create(serviceMessage);

        ResourceArchive archive = (ResourceArchive) governor.build(createdArchive.getId());
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
        ResourceArchive createdArchive = (ResourceArchive) governor.create(serviceMessage);

        ResourceArchive archive = (ResourceArchive) governor.build(createdArchive.getId());
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
