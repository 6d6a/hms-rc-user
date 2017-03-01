package ru.majordomo.hms.rc.user.test.config;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class FongoConfig  extends AbstractMongoConfiguration {
    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }
}
