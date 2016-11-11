package ru.majordomo.hms.rc.user.test.config.common;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;

@Configuration
public class ConfigStaffResourceControllerClient {
    @Bean
    public StaffResourceControllerClient staffResourceControllerClient() {
        return new StaffResourceControllerClient() {
            @Override
            public Server getActiveHostingServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
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

            @Override
            public Server getServerByServiceId(@PathVariable("serviceId") String serviceId) {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                return server;
            }
        };
    }

}
