package ru.majordomo.hms.rc.user.test.config.amqp;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.amqp.PersonAMQPController;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;


@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigPersonAMQPControllerTest extends AbstractMongoConfiguration {
    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson();
    }
    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
        return new JettyEmbeddedServletContainerFactory(0);
    }
    @Bean
    public PersonAMQPController personAMQPController() {
        return new PersonAMQPController();
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }
}
