package ru.majordomo.hms.rc.user.configurations;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean(name = "pdnsDataSourceProperties")
    @ConfigurationProperties("spring.datasource")
    @Primary
    public DataSourceProperties pdnsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pdnsDataSource")
    @Primary
    public HikariDataSource pdnsDataSource(@Qualifier("pdnsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "pdnsNamedParameterJdbcTemplate")
    @Primary
    @Autowired
    public NamedParameterJdbcTemplate pdnsNamedParameterJdbcTemplate(@Qualifier("pdnsDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(name = "billingDataSourceProperties")
    @ConfigurationProperties("datasource.billing")
    public DataSourceProperties billingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "billingDataSource")
    public HikariDataSource billingDataSource(@Qualifier("billingDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "billingNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate billingNamedParameterJdbcTemplate(@Qualifier("billingDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(name = "registrantDataSourceProperties")
    @ConfigurationProperties("datasource.registrant")
    public DataSourceProperties registrantDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "registrantDataSource")
    public HikariDataSource registrantDataSource(@Qualifier("registrantDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "registrantNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate registrantNamedParameterJdbcTemplate(@Qualifier("registrantDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
