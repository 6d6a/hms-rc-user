package ru.majordomo.hms.rc.user.resources.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;

import javax.annotation.Nullable;
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
    public Long insert(DNSResourceRecord record) {
        String query = "insert into records (domain_id, prio, `type`, ttl, `name`, content) values (:domain_id, :prio, :type, :ttl, :name, :content)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("type", Types.VARCHAR);
        parameters.addValue("type", record.getRrType());
        parameters.addValue("domain_id", record.getDomainId());
        parameters.addValue("prio", record.getPrio());
        parameters.addValue("ttl", record.getTtl());
        parameters.addValue("name", record.getOwnerName());
        parameters.addValue("content", record.getData());

        KeyHolder holder = new GeneratedKeyHolder();

        jdbcTemplate.update(query, parameters, holder, new String[]{"id"});
        return holder.getKey().longValue();
    }

    public void save(DNSResourceRecord record) {
        record.setDomainId(getDomainIDByDomainNameOrCreate(record.getName()));
        if (record.getRecordId() == null) {
            record.setRecordId(insert(record));
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
        String query = "SELECT r.*, d.name as domain_name FROM records r LEFT JOIN domains d ON d.id = r.domain_id WHERE r.id = :recordId";
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
        String query = "SELECT r.*, d.name as domain_name FROM records r LEFT JOIN domains d ON d.id = r.domain_id WHERE r.name = :domainName AND r.type IN (:types)";
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
        record.setName(rs.getString("domain_name"));
        record.setData(rs.getString("content"));
        record.setDomainId(rs.getLong("domain_id"));
        record.setPrio(rs.getLong("prio") == 0 ? null : rs.getLong("prio"));
        record.setTtl(rs.getLong("ttl"));
        record.setRrType(DNSResourceRecordType.valueOf(rs.getString("type")));
        return record;
    }

    @Override
    public List<DNSResourceRecord> getByDomainName(String domainName) {
        String query = "SELECT r.*, d.name as domain_name FROM records r LEFT JOIN domains d ON d.id = r.domain_id where d.name = :domainName";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        List<DNSResourceRecord> records;
        records = jdbcTemplate.query(query, parameters, this::rowMap);
        return records;
    }

    @Override
    public List<DNSResourceRecord> getNSRecords(String domainName) {
        String query = "SELECT r.*, d.name as domain_name FROM records r LEFT JOIN domains d ON d.id = r.domain_id where d.name = :domainName and r.type = :type";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        parameters.addValue("type", DNSResourceRecordType.NS);
        List<DNSResourceRecord> records;
        records = jdbcTemplate.query(query, parameters, this::rowMap);
        return records;
    }

    @Nullable
    private Long getDomainIDByDomainName(String domainName) {
        String query = "SELECT d.id FROM domains d WHERE d.name = :domainName";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);
        try {
            return jdbcTemplate.queryForObject(query, parameters, Long.class);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Nullable
    private Long getDomainIDByDomainNameOrCreate(String domainName) {
        Long result = getDomainIDByDomainName(domainName);
        return result != null ? result : initDomain(domainName);
    }

    /**
     * Создает основную DNS-запись домена. Если домен уже создан, вернет его ид и удалит старые записи
     * @param domainName - имя домена в формате punycode
     * @return - вернет Id домена или null
     */
    @Override
    @Nullable
    public Long initDomain(String domainName) throws DataAccessException {
        String query = "INSERT INTO domains (name, uid) VALUES (:domainName, '0')";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainName", domainName);

        KeyHolder holder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(query, parameters, holder, new String[]{"id"});
        } catch (DuplicateKeyException ex) {
            Long oldDomainId = getDomainIDByDomainName(domainName);
            if (oldDomainId != null) {
                dropRecords(oldDomainId);
            }
            return oldDomainId;
        }
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    private void dropRecords(long domainId) {
        String recordsQuery = "DELETE FROM records WHERE domain_id = :domainId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainId", domainId);

        jdbcTemplate.update(recordsQuery, parameters);
    }

    public void dropDomain(String domainName) {
        Long domainId = getDomainIDByDomainName(domainName);
        if (domainId == null) {
            return;
        }
        dropRecords(domainId);

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.registerSqlType("types", Types.VARCHAR);
        parameters.addValue("domainId", domainId);
        String domainsQuery = "DELETE FROM domains WHERE id = :domainId";
        jdbcTemplate.update(domainsQuery, parameters);
    }

    public void switchByDomainName(String domainName, Boolean switchedOn) {
        String active = switchedOn ? "1" : "0";
        Long domainId = getDomainIDByDomainNameOrCreate(domainName);
        String query = "UPDATE records SET active = :active WHERE domain_id = :domainId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("active", active);
        parameters.addValue("domainId", domainId);
        jdbcTemplate.update(query, parameters);

        query = "UPDATE domains SET active = :active WHERE id = :domainId";
        jdbcTemplate.update(query, parameters);
    }
}
