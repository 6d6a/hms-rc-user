package ru.majordomo.hms.rc.user.test.config.governors;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigGovernorOfDatabase extends AbstractMongoConfiguration {
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public GovernorOfDatabase governorOfDatabase() {
        return new GovernorOfDatabase();
    }

    @Bean
    public GovernorOfDatabaseUser governorOfDatabaseUser() {
        return new GovernorOfDatabaseUser();
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
