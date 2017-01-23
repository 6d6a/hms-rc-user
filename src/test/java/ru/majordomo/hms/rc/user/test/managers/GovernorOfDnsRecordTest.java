package ru.majordomo.hms.rc.user.test.managers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAO;
import ru.majordomo.hms.rc.user.resources.DAO.DNSDomainDAOImpl;
import ru.majordomo.hms.rc.user.resources.DAO.DNSResourceRecordDAOImpl;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernorOfDnsRecord;

import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

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
        List<DNSResourceRecord> records = dnsResourceRecordDAO.getByDomainName("testsiten3.ru");
        System.out.println(records);
    }
}
