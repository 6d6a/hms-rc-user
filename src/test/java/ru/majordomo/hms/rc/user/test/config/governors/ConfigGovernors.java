package ru.majordomo.hms.rc.user.test.config.governors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;
import ru.majordomo.hms.rc.user.configurations.MysqlSessionVariablesConfig;
import ru.majordomo.hms.rc.user.managers.*;
import ru.majordomo.hms.rc.user.service.CounterService;

@EnableConfigurationProperties(DefaultWebSiteSettings.class)
public class ConfigGovernors {
    @Bean
    public GovernorOfWebSite governorOfWebSite() {
        return new GovernorOfWebSite();
    }

    @Bean
    public GovernorOfDnsRecord governorOfDnsRecord() {
        return new GovernorOfDnsRecord();
    }

    @Bean
    public GovernorOfDomain governorOfDomain() {
        return new GovernorOfDomain();
    }

    @Bean
    public GovernorOfSSLCertificate governorOfSSLCertificate() {
        return new GovernorOfSSLCertificate();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson();
    }

    @Bean
    public GovernorOfDatabase governorOfDatabase() {
        return new GovernorOfDatabase();
    }

    @Bean
    public GovernorOfDatabaseUser governorOfDatabaseUser() {
        return new GovernorOfDatabaseUser();
    }

    @Bean
    public GovernorOfResourceArchive governorOfResourceArchive() {
        return new GovernorOfResourceArchive();
    }

    @Bean
    public GovernorOfFTPUser governorOfFTPUser() {
        return new GovernorOfFTPUser();
    }

    @Bean
    public GovernorOfMailbox governorOfMailbox() {
        return new GovernorOfMailbox();
    }

    @Bean
    public GovernorOfRedirect getGovernorOfRedirect() {
        return new GovernorOfRedirect();
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
