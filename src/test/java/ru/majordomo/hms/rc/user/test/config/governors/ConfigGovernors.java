package ru.majordomo.hms.rc.user.test.config.governors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;
import ru.majordomo.hms.rc.user.configurations.MysqlSessionVariablesConfig;
import ru.majordomo.hms.rc.user.managers.*;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.service.CounterService;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;

@EnableConfigurationProperties(DefaultWebSiteSettings.class)
public class ConfigGovernors {

    private OperationOversightRepository<WebSite> websiteOvsRep;
    private OperationOversightRepository<DNSResourceRecord> dnsRecordOvsRep;
    private OperationOversightRepository<Domain> domainOvsRep;
    private OperationOversightRepository<SSLCertificate> sslCertOvsRep;
    private OperationOversightRepository<UnixAccount> unixAcOvsRep;
    private OperationOversightRepository<Person> personOvsRep;
    private OperationOversightRepository<Database> databaseOvsRep;
    private OperationOversightRepository<DatabaseUser> databaseUserOvsRep;
    private OperationOversightRepository<ResourceArchive> resourceArchOvsRep;
    private OperationOversightRepository<FTPUser> ftpUserOvsRep;
    private OperationOversightRepository<Mailbox> mailboxOvsRep;
    private OperationOversightRepository<Redirect> redirectOvsRep;

    @Autowired
    private void ConfigGovernors(
            OperationOversightRepository<WebSite> websiteOvsRep,
            OperationOversightRepository<DNSResourceRecord> dnsRecordOvsRep,
            OperationOversightRepository<Domain> domainOvsRep,
            OperationOversightRepository<SSLCertificate> sslCertOvsRep,
            OperationOversightRepository<UnixAccount> unixAcOvsRep,
            OperationOversightRepository<Person> personOvsRep,
            OperationOversightRepository<Database> databaseOvsRep,
            OperationOversightRepository<DatabaseUser> databaseUserOvsRep,
            OperationOversightRepository<ResourceArchive> resourceArchOvsRep,
            OperationOversightRepository<FTPUser> ftpUserOvsRep,
            OperationOversightRepository<Mailbox> mailboxOvsRep,
            OperationOversightRepository<Redirect> redirectOvsRep) {
        this.websiteOvsRep = websiteOvsRep;
        this.dnsRecordOvsRep = dnsRecordOvsRep;
        this.domainOvsRep = domainOvsRep;
        this.sslCertOvsRep = sslCertOvsRep;
        this.unixAcOvsRep = unixAcOvsRep;
        this.personOvsRep = personOvsRep;
        this.databaseOvsRep = databaseOvsRep;
        this.databaseUserOvsRep = databaseUserOvsRep;
        this.resourceArchOvsRep = resourceArchOvsRep;
        this.ftpUserOvsRep = ftpUserOvsRep;
        this.mailboxOvsRep = mailboxOvsRep;
        this.redirectOvsRep = redirectOvsRep;
    }

    @Bean
    public PmFeignClient personmgr() {return new PmFeignClient() {
        @Override
        public ServiceMessage sendNotificationToClient(ServiceMessage message) {
            return null;
        }

        @Override
        public String sendPhpMailNotificationToClient(String accountId) {
            return null;
        }
    }; }
    @Bean
    public GovernorOfWebSite governorOfWebSite() {
        return new GovernorOfWebSite(websiteOvsRep);
    }

    @Bean
    public GovernorOfDnsRecord governorOfDnsRecord() {
        return new GovernorOfDnsRecord(dnsRecordOvsRep);
    }

    @Bean
    public GovernorOfDomain governorOfDomain() {
        return new GovernorOfDomain(domainOvsRep);
    }

    @Bean
    public GovernorOfSSLCertificate governorOfSSLCertificate() {
        return new GovernorOfSSLCertificate(sslCertOvsRep);
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount(unixAcOvsRep);
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson(personOvsRep);
    }

    @Bean
    public GovernorOfDatabase governorOfDatabase() {
        return new GovernorOfDatabase(databaseOvsRep);
    }

    @Bean
    public GovernorOfDatabaseUser governorOfDatabaseUser() {
        return new GovernorOfDatabaseUser(databaseUserOvsRep);
    }

    @Bean
    public GovernorOfResourceArchive governorOfResourceArchive() {
        return new GovernorOfResourceArchive(resourceArchOvsRep);
    }

    @Bean
    public GovernorOfFTPUser governorOfFTPUser() {
        return new GovernorOfFTPUser(ftpUserOvsRep);
    }

    @Bean
    public GovernorOfMailbox governorOfMailbox() {
        return new GovernorOfMailbox(mailboxOvsRep);
    }

    @Bean
    public GovernorOfRedirect getGovernorOfRedirect() {
        return new GovernorOfRedirect(redirectOvsRep);
    }

    @Bean
    public ServletWebServerFactory embeddedServletContainerFactory() {
        return new JettyServletWebServerFactory(0);
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public CounterService counterService() {
        return new CounterService();
    }

    @Bean
    MysqlSessionVariablesConfig mysqlSessionVariablesConfig() {
        return new MysqlSessionVariablesConfig();
    }
}
