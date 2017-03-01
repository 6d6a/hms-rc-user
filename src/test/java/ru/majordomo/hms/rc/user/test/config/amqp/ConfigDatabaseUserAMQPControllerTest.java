package ru.majordomo.hms.rc.user.test.config.amqp;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.bson.types.ObjectId;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ru.majordomo.hms.rc.user.api.amqp.DatabaseUserAMQPController;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigDatabaseUserAMQPControllerTest extends AbstractMongoConfiguration {
    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
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
    public GovernorOfWebSite governorOfWebSite() {
        return new GovernorOfWebSite();
    }
    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
        return new JettyEmbeddedServletContainerFactory(0);
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }
}
