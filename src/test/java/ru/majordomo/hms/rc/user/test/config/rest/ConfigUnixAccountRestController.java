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
import ru.majordomo.hms.rc.user.api.http.PersonRestController;
import ru.majordomo.hms.rc.user.api.http.UnixAccountRESTController;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebMvc
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigUnixAccountRestController extends AbstractMongoConfiguration {
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
    public UnixAccountRESTController unixAccountRESTController() {
        return new UnixAccountRESTController();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public StaffResourceControllerClient staffResourceControllerClient() {
        return new StaffResourceControllerClient() {
            @Override
            public Server getActiveHostingServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());;
                return server;
            }

            @Override
            public Server getActiveDatabaseServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                return server;
            }

            @Override
            public Server getActiveMailboxServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                return server;
            }

            @Override
            public Server getServerById(@PathVariable("serverId") String serverId) {
                Server server = new Server();
                server.setId(serverId);
                return server;
            }
        };
    }
}
