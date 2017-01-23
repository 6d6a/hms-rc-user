package ru.majordomo.hms.rc.user.resources.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DNSDomainDAOImpl implements DNSDomainDAO {
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DNSDomainDAOImpl(@Qualifier("pdnsNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(String domainName) {

    }

    @Override
    public void update(String domainName) {

    }

    @Override
    public void delete(String domainName) {

    }

    @Override
    public void switchDomain(String domainName) {

    }

    @Override
    public Boolean hasDomainRecord(String domainName) {
        return true;
    }
}
