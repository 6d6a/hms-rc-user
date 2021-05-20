package ru.majordomo.hms.rc.user.test.config.amqp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resources.Person;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigPersonAMQPControllerTest {

    private OperationOversightRepository<Person> personOvsRep;

    @Autowired
    public ConfigPersonAMQPControllerTest(OperationOversightRepository<Person> personOvsRep) {
        this.personOvsRep = personOvsRep;
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson(personOvsRep);
    }
    @Bean
    public ServletWebServerFactory embeddedServletContainerFactory() {
        return new JettyServletWebServerFactory(0);
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }
}
