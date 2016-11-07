package ru.majordomo.hms.rc.user.test.config.governors;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigGovernorOfUnixAccount extends AbstractMongoConfiguration {
    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
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
    public StaffResourceControllerClient staffResourceControllerClient() {
        return new StaffResourceControllerClient() {
            @Override
            public List<Server> getActiveHostingServers() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                List<Server> servers = new ArrayList<>();
                servers.add(server);
                return servers;
            }

            @Override
            public List<Server> getActiveDatabaseServers() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                List<Server> servers = new ArrayList<>();
                servers.add(server);
                return servers;
            }

            @Override
            public List<Server> getActiveMailboxServers() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                List<Server> servers = new ArrayList<>();
                servers.add(server);
                return servers;
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
