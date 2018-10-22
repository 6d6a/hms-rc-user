package ru.majordomo.hms.rc.user.configurations;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import ru.majordomo.hms.rc.user.mappers.StringToAddressConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {
    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<>();
        converterList.add(new StringToAddressConverter());
        return new MongoCustomConversions(converterList);
    }

    @Bean
    @Primary
    public MongoClient mongo() throws Exception {
        return new MongoClient(new MongoClientURI(mongodbUri));
    }

    @Bean("jongoMongoClient")
    public MongoClient jongoMongoClient() throws Exception {
        return new MongoClient(new MongoClientURI(mongodbUri));
    }
}
