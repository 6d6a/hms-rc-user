package ru.majordomo.hms.rc.user.test.managers;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAO;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAOImpl;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDnsRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigDomainRegistrarClient.class,
                ConfigStaffResourceControllerClient.class,
                ConfigGovernorOfDnsRecord.class,
                DNSResourceRecordDAOImpl.class,
                DNSDomainDAOImpl.class,
                DatabaseConfig.class
        },
        webEnvironment = NONE,
        properties = {
        }
)
public class GovernorOfDnsRecordTest {
    @Autowired
    private GovernorOfDnsRecord governorOfDnsRecord;

    @Autowired
    private DNSResourceRecordDAOImpl dnsResourceRecordDAO;

    @Test
    public void getByDomainName() throws Exception {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "example.com");
        keyValue.put("accountId", "example.com");
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        assertThat(records.get(1).getOwnerName(), is("example.com"));
        assertThat(records.get(1).getData(), is("8.8.8.8"));
    }

    @Test
    public void initDomain() throws Exception {
        Domain domain = new Domain();
        domain.setName("new-domain.ru");
        governorOfDnsRecord.initDomain(domain);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "new-domain.ru");
        keyValue.put("accountId", "0");
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        assertThat(records.size(), is(4));
        assertThat(records.get(0).getRrType(), is(DNSResourceRecordType.SOA));
    }

    @Test
    public void storeNew() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("name", "example.com");
        serviceMessage.addParam("ownerName", "*.example.com");
        serviceMessage.addParam("type", "A");
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3600L);
        governorOfDnsRecord.create(serviceMessage);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "example.com");
        keyValue.put("accountId", "0");
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        System.out.println(records);
    }

//    @Test(expected = ResourceNotFoundException.class)
//    public void delete() throws Exception {
//        governorOfDnsRecord.drop("2");
//        governorOfDnsRecord.build("2");
//    }

    @Test(expected = ParameterValidateException.class)
    public void buildBadResourceId() throws Exception {
        governorOfDnsRecord.build("notnumericid");
    }

    @Test
    public void findOne() throws Exception {
        DNSResourceRecord record = dnsResourceRecordDAO.findOne(4L);
        System.out.println(record);
    }

    @Test
    public void getDomainNameByRecordId() throws Exception {
        String domainName = dnsResourceRecordDAO.getDomainNameByRecordId(7L);
        assertThat(domainName, is("example.com"));
    }

    @Test
    public void storeExistent() throws Exception {
        DNSResourceRecord record = new DNSResourceRecord();
        record.setRecordId(2L);
        record.setName("example.com");
        record.setOwnerName("example.com");
        record.setRrType(DNSResourceRecordType.A);
        record.setTtl(300L);
        record.setData("78.108.87.68");
        governorOfDnsRecord.store(record);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("name", "example.com");
        keyValue.put("accountId", "0");
        List<DNSResourceRecord> records = (List<DNSResourceRecord>) governorOfDnsRecord.buildAll(keyValue);
        System.out.println(records);
    }

    @Test
    public void update() throws Exception {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setAccountId(ObjectId.get().toString());
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.addParam("resourceId", 2L);
        serviceMessage.addParam("name", "example.com");
        serviceMessage.addParam("ownerName", "sub.example.com");
        serviceMessage.addParam("data", "78.108.80.36");
        serviceMessage.addParam("ttl", 3700L);
        governorOfDnsRecord.update(serviceMessage);

        DNSResourceRecord record = (DNSResourceRecord) governorOfDnsRecord.build("2");
        System.out.println(record);
    }
}
