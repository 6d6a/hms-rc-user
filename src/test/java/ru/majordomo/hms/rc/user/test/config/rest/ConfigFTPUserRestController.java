package ru.majordomo.hms.rc.user.test.config.rest;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.ServiceType;
import ru.majordomo.hms.rc.user.api.http.DatabaseRestController;
import ru.majordomo.hms.rc.user.api.http.DomainRestController;
import ru.majordomo.hms.rc.user.api.http.FTPUserRestController;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrar;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.FTPUser;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebMvc
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigFTPUserRestController extends AbstractMongoConfiguration {
    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
        return new JettyEmbeddedServletContainerFactory(0);
    }

    @Override
    protected String getDatabaseName() {
        return "rc-user-" + ObjectId.get().toString();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Fongo(getDatabaseName()).getMongo();
    }

    @Bean
    public FTPUserRestController ftpUserRestController() {
        return new FTPUserRestController();
    }

    @Bean
    public GovernorOfFTPUser governorOfFTPUser() {
        return new GovernorOfFTPUser();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
    }
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }
}
