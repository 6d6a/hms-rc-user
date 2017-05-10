package ru.majordomo.hms.rc.user.configurations;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.majordomo.hms.rc.user.mappers.PersonWriteConverter;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DatabaseConfig extends AbstractMongoConfiguration{

    private String mongoDatabaseName;
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    public void setMongoDatabaseName(String mongoDatabaseName) {
        this.mongoDatabaseName = mongoDatabaseName;
    }

    @Value("${spring.data.mongodb.uri}")
    public void setMongoUri(String mongoUri) {
        this.mongoUri = mongoUri;
    }



    @Bean(name = "pdnsDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource pdnsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "pdnsJdbcTemplate")
    @Autowired
    public JdbcTemplate pdnsJdbcTemplate(@Qualifier("pdnsDataSource") DataSource pdnsDataSource) {
        return new JdbcTemplate(pdnsDataSource);
    }

    @Bean(name = "pdnsNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate pdnsNamedParameterJdbcTemplate(@Qualifier("pdnsDataSource") DataSource pdnsDataSource) {
        return new NamedParameterJdbcTemplate(pdnsDataSource);
    }

    @Bean(name = "billingDataSource")
    @ConfigurationProperties(prefix="datasource.billing")
    public DataSource billingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "billingJdbcTemplate")
    @Autowired
    public JdbcTemplate billingJdbcTemplate(@Qualifier("billingDataSource") DataSource billingDataSource) {
        return new JdbcTemplate(billingDataSource);
    }

    @Bean(name = "billingNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate billingNamedParameterJdbcTemplate(@Qualifier("billingDataSource") DataSource billingDataSource) {
        return new NamedParameterJdbcTemplate(billingDataSource);
    }

    @Bean(name = "registrantDataSource")
    @ConfigurationProperties(prefix="datasource.registrant")
    public DataSource registrantDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "registrantJdbcTemplate")
    @Autowired
    public JdbcTemplate registrantJdbcTemplate(@Qualifier("registrantDataSource") DataSource registrantDataSource) {
        return new JdbcTemplate(registrantDataSource);
    }

    @Bean(name = "registrantNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate registrantNamedParameterJdbcTemplate(@Qualifier("registrantDataSource") DataSource registrantDataSource) {
        return new NamedParameterJdbcTemplate(registrantDataSource);
    }

    @Override
    protected String getDatabaseName() {
        return this.mongoDatabaseName;
    }

    @Override
    public Mongo mongo() throws Exception {
        return new MongoClient(this.mongoUri);
    }

    @Bean
    @Override
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<Converter<?, ?>>();
        converterList.add(new PersonWriteConverter());
        return new CustomConversions(converterList);
    }
}
