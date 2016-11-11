package ru.majordomo.hms.rc.user.test.config.common;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.ServiceType;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;

import java.util.ArrayList;
import java.util.List;

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

            @Override
            public List<Service> getWebsiteServicesByServerIdAndServiceType(@PathVariable("serverId") String serverId) {
                List<Service> services = new ArrayList<>();

                Service service = new Service();
                service.setId(ObjectId.get().toString());

                ServiceType serviceType = new ServiceType();
                serviceType.setName("WEBSITE_APACHE2_PHP56_DEFAULT");
                service.setServiceType(serviceType);

                services.add(service);

                return services;
            }
        };
    }

}
