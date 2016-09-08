package ru.majordomo.hms.rc.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UsersResourceControllerApplication.class)
public class GovernorOfDomainTest {
    @Autowired
    DomainRepository domainRepository;

    @Test
    public void createTest() {
        Domain domain = new Domain();
        domain.setName("majordomo.ru");
        domainRepository.save(domain);
        System.out.println(domain.toString());
    }
}
