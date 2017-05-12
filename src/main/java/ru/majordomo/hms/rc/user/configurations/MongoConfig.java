package ru.majordomo.hms.rc.user.configurations;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import ru.majordomo.hms.rc.user.mappers.PersonWriteConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {
    @Bean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<Converter<?, ?>>();
        converterList.add(new PersonWriteConverter(objectFactory()));
        return new CustomConversions(converterList);
    }

    @Bean
    public ObjectFactory<MappingMongoConverter> objectFactory() {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(new SimpleMongoDbFactory(new MongoClient(), "usersResourceController"));
        return () -> new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
    }
}
