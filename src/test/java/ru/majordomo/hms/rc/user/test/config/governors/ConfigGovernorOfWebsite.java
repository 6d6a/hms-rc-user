package ru.majordomo.hms.rc.user.test.config.governors;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;
import ru.majordomo.hms.rc.user.managers.*;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
@EnableConfigurationProperties(DefaultWebSiteSettings.class)
public class ConfigGovernorOfWebsite extends AbstractMongoConfiguration {
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public GovernorOfWebSite governorOfWebSite() {
        return new GovernorOfWebSite(null);
    }

    @Bean
    public GovernorOfDnsRecord governorOfDnsRecord() {
        return new GovernorOfDnsRecord(null);
    }

    @Bean
    public GovernorOfDomain governorOfDomain() {
        return new GovernorOfDomain(null);
    }

    @Bean
    public GovernorOfSSLCertificate governorOfSSLCertificate() {
        return new GovernorOfSSLCertificate(null);
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount(null);
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson(null);
    }

    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public MongoClient mongoClient() {
        return new Fongo(getDatabaseName()).getMongo();
    }
}
