package ru.majordomo.hms.rc.user.test.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAOImpl;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean(name = "pdnsDataSource")
    @Primary
    public DataSource pdnsDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://dev.majordomo.ru/pdns")
                .driverClassName("com.mysql.jdbc.Driver")
                .username("root")
                .password("cfg0;0r")
                .build();
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

    @Bean
    public DNSDomainDAOImpl DNSDomainDAOImpl() {
        return new DNSDomainDAOImpl(pdnsNamedParameterJdbcTemplate(pdnsDataSource()));
    }

    @Bean
    DNSResourceRecordDAOImpl DNSResourceRecordDAOImpl() {
        return new DNSResourceRecordDAOImpl(pdnsNamedParameterJdbcTemplate(pdnsDataSource()));
    }
}