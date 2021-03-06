package ru.majordomo.hms.rc.user.test.config.rest;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.majordomo.hms.rc.user.api.http.PersonRestController;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;

@Configuration
@EnableWebMvc
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigPersonRestController extends AbstractMongoConfiguration {
    @Bean
    public ServletWebServerFactory embeddedServletContainerFactory() {
        return new JettyServletWebServerFactory(0);
    }

    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public MongoClient mongoClient() {
        return new Fongo(getDatabaseName()).getMongo();
    }

    @Bean
    public PersonRestController personRestController() {
        return new PersonRestController();
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson(null);
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }
}
