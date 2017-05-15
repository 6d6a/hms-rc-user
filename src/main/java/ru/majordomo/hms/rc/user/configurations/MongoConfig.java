package ru.majordomo.hms.rc.user.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import ru.majordomo.hms.rc.user.mappers.StringToAddressConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {
    @Bean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<Converter<?, ?>>();
        converterList.add(new StringToAddressConverter());
        return new CustomConversions(converterList);
    }
}
