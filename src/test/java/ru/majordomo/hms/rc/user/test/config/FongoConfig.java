package ru.majordomo.hms.rc.user.test.config;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
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
    @Bean
    public MongoClient mongoClient() {
        return new Fongo(getDatabaseName()).getMongo();
    }

    @Bean("jongoMongoClient")
    public MongoClient jongoMongoClient() throws Exception {
        return mongoClient();
    }
}
