package ru.majordomo.hms.rc.user.test.config.governors;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.*;
import ru.majordomo.hms.rc.user.resources.Domain;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigGovernorOfMailbox extends AbstractMongoConfiguration {
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public GovernorOfMailbox governorOfMailbox() {
        return new GovernorOfMailbox();
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
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
    }

    @Bean
    public GovernorOfDnsRecord governorOfDnsRecord() {
        return new GovernorOfDnsRecord();
    }

    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }
}
