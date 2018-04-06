package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.ValidationConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,
                ValidationConfig.class,

                ConfigGovernors.class
        },
        webEnvironment = NONE,
        properties = {
                "resources.quotable.warnPercent.mailbox=90"
        }
)
public class GovernorOfDnsRecordTest {
    @Autowired
    private GovernorOfDnsRecord governorOfDnsRecord;

    @Autowired
    private DNSResourceRecordDAOImpl dnsResourceRecordDAO;

    @Autowired
    private UnixAccountRepository unixAccountRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private PersonRepository personRepository;

    private Collection<UnixAccount> unixAccounts;
    private List<Domain> domains;
    private List<Person> persons;

    @Before
    public void setUp() throws Exception {
        unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts();
        unixAccountRepository.save(unixAccounts);
        persons = ResourceGenerator.generateBatchOfPerson();
        domains = ResourceGenerator.generateBatchOfDomains(persons);
        domainRepository.save(domains);
        personRepository.save(persons);
    }

    @Test
    public void getByDomainName() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        assertThat(records.size(), is(7));
    }

    @Test
    public void initDomain() throws Exception {
        Domain domain = new Domain();
        domain.setName("new-domain.ru");
        domain.setAccountId(unixAccounts.iterator().next().getAccountId());
        governorOfDnsRecord.initDomain(domain);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "new-domain.ru");
        keyValue.put("accountId", unixAccounts.iterator().next().getAccountId());

        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        assertThat(
                records.size(),
                is(10)
        );
        assertThat(
                records.get(0).getRrType(),
                is(DNSResourceRecordType.SOA)
        );
        assertThat(
                records.stream()
                        .filter(r -> r.getRrType().equals(DNSResourceRecordType.CNAME))
                        .count(),
                is(3L)
        );
    }

    @Test
    public void initDomainRF() throws Exception {
        Domain domain = new Domain();
        domain.setName("ололоевич.рф");
        domain.setAccountId(unixAccounts.iterator().next().getAccountId());
        governorOfDnsRecord.initDomain(domain);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "ололоевич.рф");
        keyValue.put("accountId", unixAccounts.iterator().next().getAccountId());
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);

        assertThat(
                records.size(),
                is(10)
        );
        assertThat(
                records.get(0).getRrType(),
                is(DNSResourceRecordType.SOA)
        );
        assertThat(
                records.stream()
                        .filter(r -> r.getRrType().equals(DNSResourceRecordType.A))
                        .collect(Collectors.toList())
                        .get(0).getData(),
                is("78.108.80.185")
        );
        assertThat(
                records.stream()
                        .filter(r -> r.getRrType().equals(DNSResourceRecordType.CNAME))
                        .count(),
                is(3L)
        );
    }

    @Test
    public void create() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());

        List<DNSResourceRecord> recordsBefore = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("ownerName", "*." + domains.get(0).getName());
        serviceMessage.addParam("type", "A");
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3600L);
        governorOfDnsRecord.create(serviceMessage);

        List<DNSResourceRecord> recordsAfter = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        assertThat(recordsAfter.size(), is(recordsBefore.size() + 1));
    }

    @Test(expected = ConstraintViolationException.class)
    public void createBad() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());

        List<DNSResourceRecord> recordsBefore = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("ownerName", "*.bad.com");
        serviceMessage.addParam("type", "A");
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3600L);
        governorOfDnsRecord.create(serviceMessage);
    }

    @Test(expected = ConstraintViolationException.class)
    public void createWithoutType() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());
        keyValue.put("accountId", domains.get(0).getAccountId());

        List<DNSResourceRecord> recordsBefore = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("ownerName", "*." + domains.get(0).getName());
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3600L);
        governorOfDnsRecord.create(serviceMessage);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void delete() throws Exception {
        governorOfDnsRecord.drop("7");
        governorOfDnsRecord.build("7");
    }

    @Test(expected = ParameterValidationException.class)
    public void buildBadResourceId() throws Exception {
        governorOfDnsRecord.build("notnumericid");
    }

    @Test
    public void getDomainNameByRecordId() throws Exception {
        String domainName = dnsResourceRecordDAO.getDomainNameByRecordId(2L);
        assertThat(domainName, is(domains.get(0).getName()));
    }

    @Test
    public void storeExistent() throws Exception {
        DNSResourceRecord record = new DNSResourceRecord();
        record.setRecordId(2L);
        record.setName(domains.get(0).getName());
        record.setOwnerName(domains.get(0).getName());
        record.setRrType(DNSResourceRecordType.A);
        record.setTtl(300L);
        record.setData("78.108.87.68");
        governorOfDnsRecord.store(record);

        DNSResourceRecord newRecord = governorOfDnsRecord.build("2");
        assertThat(newRecord.getRecordId(), is(2L));
        assertThat(newRecord.getTtl(), is(300L));
        assertThat(newRecord.getData(), is("78.108.87.68"));
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(domains.get(0).getAccountId());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", "2");
        serviceMessage.addParam("name", domains.get(0).getName());
        serviceMessage.addParam("ownerName", "sub2." + domains.get(0).getName());
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3700L);
        governorOfDnsRecord.update(serviceMessage);

        DNSResourceRecord record = governorOfDnsRecord.build("2");
        assertThat(record.getRecordId(), is(2L));
        assertThat(record.getName(), is(domains.get(0).getName()));
        assertThat(record.getOwnerName(), is("sub2." + domains.get(0).getName()));
        assertThat(record.getData(), is("78.108.80.36"));
        assertThat(record.getTtl(), is(3700L));
    }

    @Test
    public void switchOnOff() throws Exception {
        dnsResourceRecordDAO.switchByDomainName(domains.get(0).getName(), false);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", domains.get(0).getName());

        Collection<DNSResourceRecord> records = governorOfDnsRecord.buildAll(keyValue);
        records.forEach(record -> assertThat(record.getSwitchedOn(), is(false)));

        dnsResourceRecordDAO.switchByDomainName(domains.get(0).getName(), true);

        records = governorOfDnsRecord.buildAll(keyValue);
        records.forEach(record -> assertThat(record.getSwitchedOn(), is(true)));
    }

    @Test
    public void findOne() throws Exception {
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(4L);
        assertThat(record.getRecordId(), is(4L));
    }
}
