package ru.majordomo.hms.rc.user.resources.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Service
public class DNSResourceRecordDAOImpl implements DNSResourceRecordDAO {
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DNSResourceRecordDAOImpl(@Qualifier("pdnsNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void update(DNSResourceRecord record) {
        String query = "update records set domain_id = :domain_id, prio = :prio, `type` = :type, ttl = :ttl, `name` = :name, content = :data where id = :id";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("type", Types.VARCHAR);
        parameters.addValue("type", record.getRrType());
        parameters.addValue("domain_id", record.getDomainId());
        parameters.addValue("prio", record.getPrio());
        parameters.addValue("ttl", record.getTtl());
        parameters.addValue("name", record.getOwnerName());
        parameters.addValue("id", record.getRecordId());
        parameters.addValue("data", record.getData());
        jdbcTemplate.update(query, parameters);
    }

    @Override
    public void insert(DNSResourceRecord record) {
        String query = "insert into records (domain_id, prio, `type`, ttl, `name`, content) values (:domain_id, :prio, :type, :ttl, :name, :content)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("type", Types.VARCHAR);
        parameters.addValue("type", record.getRrType());
        parameters.addValue("domain_id", record.getDomainId());
        parameters.addValue("prio", record.getPrio());
        parameters.addValue("ttl", record.getTtl());
        parameters.addValue("name", record.getOwnerName());
        parameters.addValue("content", record.getData());
        jdbcTemplate.update(query, parameters);
    }

    public void save(DNSResourceRecord record) {
        record.setDomainId(getDomainIDByDomainName(record.getName()));
        if (record.getRecordId() == null) {
            insert(record);
        } else {
            update(record);
        }
    }

    @Override
    public void delete(DNSResourceRecord record) {
        Long id = record.getRecordId();
        String query = "DELETE FROM records where id = :recordId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("recordId", id);
        jdbcTemplate.update(query, parameters);
    }

    @Override
    public void delete(Long recordId) {
        String query = "DELETE FROM records where id = :recordId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("recordId", recordId);
        jdbcTemplate.update(query, parameters);
    }

    @Override
    public DNSResourceRecord findOne(Long recordId) {
        String query = "SELECT * FROM records WHERE id = :recordId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("recordId", recordId);
        DNSResourceRecord record;
        try {
            record = jdbcTemplate.queryForObject(query, parameters, this::rowMap);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Не найдено DNS-записи с ID " + recordId);
        }
        return record;
    }

    @Override
    public String getDomainNameByRecordId(Long recordId) {
        String query = "SELECT d.name FROM domains d JOIN records r ON r.domain_id = d.id WHERE r.id = :recordId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("recordId", recordId);
        return jdbcTemplate.queryForObject(query, parameters, String.class);
    }

    @Override
    public boolean insertByDomainName(String domainName, DNSResourceRecord record) {
        String query = "SELECT d.id FROM domains d WHERE d.name = :domainName";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("domainName", domainName);
        Long domainId;
        try {
            domainId = jdbcTemplate.queryForObject(query, parameters, Long.class);
        } catch (DataAccessException e) {
            e.printStackTrace();
            return false;
        }
        if (domainId == null) {
            return false;
        }
        query = "insert into records (domain_id, prio, `type`, ttl, `name`, content) values (:domain_id, :prio, :type, :ttl, :name, :content)";
        parameters = new MapSqlParameterSource();
        parameters.registerSqlType("type", Types.VARCHAR);
        parameters.addValue("type", record.getRrType());
        parameters.addValue("domain_id", domainId);
        parameters.addValue("prio", record.getPrio());
        parameters.addValue("ttl", record.getTtl());
        parameters.addValue("name", record.getOwnerName());
        parameters.addValue("content", record.getData());
        jdbcTemplate.update(query, parameters);
        return true;
    }

    @Override
    public List<DNSResourceRecord> getByDomainNameAndTypeIn(String domainName, List<DNSResourceRecordType> types) {
        String query = "SELECT r.* FROM records r WHERE r.name = :domainName AND r.type IN (:types)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("types", types);
        parameters.addValue("domainName", domainName);
        List<DNSResourceRecord> records;
        records = jdbcTemplate.query(query, parameters, this::rowMap);
        return records;
    }

    private DNSResourceRecord rowMap(ResultSet rs, int rowNum) throws SQLException {
        DNSResourceRecord record = new DNSResourceRecord();
        record.setRecordId(rs.getLong("id"));
        record.setOwnerName(rs.getString("name"));
        record.setData(rs.getString("content"));
        record.setDomainId(rs.getLong("domain_id"));
        record.setPrio(rs.getLong("prio") == 0 ? null : rs.getLong("prio"));
        record.setTtl(rs.getLong("ttl"));
        record.setRrType(DNSResourceRecordType.valueOf(rs.getString("type")));
        return record;
    }

    @Override
    public List<DNSResourceRecord> getByDomainName(String domainName) {
        String query = "SELECT r.* FROM records r LEFT JOIN domains d ON d.id = r.domain_id where d.name = :domainName";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        List<DNSResourceRecord> records;
        records = jdbcTemplate.query(query, parameters, this::rowMap);
        return records;
    }

    @Override
    public List<DNSResourceRecord> getNSRecords(String domainName) {
        String query = "SELECT r.* FROM records r LEFT JOIN domains d ON d.id = r.domain_id where d.name = :domainName and r.type = :type";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        parameters.addValue("type", DNSResourceRecordType.NS);
        List<DNSResourceRecord> records;
        records = jdbcTemplate.query(query, parameters, this::rowMap);
        return records;
    }

//    @Override
    private Long getDomainIDByDomainName(String domainName) {
        String query = "SELECT d.id FROM domains d WHERE d.name = :domainName";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        return jdbcTemplate.queryForObject(query, parameters, Long.class);
    }

    @Override
    public void initDomain(String domainName) {
        String query = "INSERT INTO domains (name, uid) VALUES (:domainName, '0')";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        jdbcTemplate.update(query, parameters);
    }
}