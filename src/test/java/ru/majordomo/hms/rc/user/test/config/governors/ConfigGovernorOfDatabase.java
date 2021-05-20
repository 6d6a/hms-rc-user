package ru.majordomo.hms.rc.user.test.config.governors;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigGovernorOfDatabase extends AbstractMongoConfiguration {
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public GovernorOfDatabase governorOfDatabase() {
        return new GovernorOfDatabase(null);
    }

    @Bean
    public GovernorOfDatabaseUser governorOfDatabaseUser() {
        return new GovernorOfDatabaseUser(null);
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
