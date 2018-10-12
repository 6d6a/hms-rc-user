package ru.majordomo.hms.rc.user.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean(name = "pdnsDataSource")
    @Primary
    public DataSource pdnsDataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:create-schema.sql")
                .build();
    }

    @Bean(name = "pdnsJdbcTemplate")
    public JdbcTemplate pdnsJdbcTemplate() {
        return new JdbcTemplate(pdnsDataSource());
    }

    @Bean(name = "pdnsNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate pdnsNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(pdnsDataSource());
    }

    @Bean
    DNSResourceRecordDAOImpl DNSResourceRecordDAOImpl() {
        return new DNSResourceRecordDAOImpl(pdnsNamedParameterJdbcTemplate());
    }
}