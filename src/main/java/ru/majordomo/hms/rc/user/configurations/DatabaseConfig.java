package ru.majordomo.hms.rc.user.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
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
}
