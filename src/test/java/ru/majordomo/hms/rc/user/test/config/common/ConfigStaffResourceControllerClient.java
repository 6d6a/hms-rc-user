package ru.majordomo.hms.rc.user.test.config.common;

import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.ServiceTemplate;
import ru.majordomo.hms.rc.staff.resources.ServiceType;
import ru.majordomo.hms.rc.staff.resources.Storage;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;

import java.util.*;

@Configuration
public class ConfigStaffResourceControllerClient {

    private final String mockedServiceId = "583300c5a94c541d14d58c84";
    private final String mockedNginxServiceId = "583300c5a94c541d14d58c86";
    private final String mockedDBServiceId = "583300c5a94c541d14d58c85";

    private Server mockedMailboxServer;

    @Bean
    public StaffResourceControllerClient staffResourceControllerClient() {
        mockedMailboxServer = new Server();
        mockedMailboxServer.setId("583300c5a94c541d14d58c87");
        mockedMailboxServer.setName("pop100500");
        Storage storage = new Storage();
        storage.setMountPoint("/homebig");
        storage.setId(ObjectId.get().toString());
        mockedMailboxServer.addStorage(storage);

        return new StaffResourceControllerClient() {
            @Override
            public Storage getActiveMailboxStorageByServerId(String serverId) {
                if (serverId.equals(mockedMailboxServer.getId())) {
                    Storage storage = new Storage();
                    storage.setId(ObjectId.get().toString());
                    storage.setMountPoint("/homebig");
                    return storage;
                } else {
                    throw new ResourceNotFoundException();
                }
            }

            @Override
            public Server getActiveHostingServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                server.setName("web100500");
                server.setServiceIds(Collections.singletonList(mockedServiceId));
                return server;
            }

            @Override
            public Map<String, String> getServerIpInfoByServerId(String serverId) {
                Map<String, String> serverIpInfo = new HashMap<>();
                serverIpInfo.put("id", "1234");
                serverIpInfo.put("serverId", serverId);
                serverIpInfo.put("primaryIp", "78.108.80.185");
                serverIpInfo.put("secondaryIp", "78.108.80.186");
                return serverIpInfo;
            }

            @Override
            public Server getActiveDatabaseServer() {
                Server server = new Server();
                server.setId(ObjectId.get().toString());
                server.setServiceIds(Collections.singletonList(mockedDBServiceId));
                return server;
            }

            @Override
            public Server getActiveMailboxServer() {
//                Server server = new Server();
//                server.setId(ObjectId.get().toString());
//                server.setName("pop100500");
//                Storage storage = new Storage();
//                storage.setId(ObjectId.get().toString());
//                storage.setMountPoint("/homebig");
//                server.addStorage(storage);
                return mockedMailboxServer;
//                return server;
            }

            @Override
            public Server getServerById(@PathVariable("serverId") String serverId) {
                Server server = new Server();
                server.setId(serverId);
                server.setServiceIds(Collections.singletonList(mockedServiceId));
                server.setName("web100500");
                if (serverId.equals(mockedMailboxServer.getId())) {
                    return mockedMailboxServer;
                }
                return server;
            }

            @Override
            public Server getServerByServiceId(@PathVariable("serviceId") String serviceId) {
                Server server = null;
                if (serviceId.equals(mockedDBServiceId) || serviceId.equals(mockedServiceId)) {
                    server = new Server();
                    server.setServiceIds(Collections.singletonList(serviceId));
                    server.setName("web100500");
                    server.setId(ObjectId.get().toString());
                }
                return server;
            }

            @Override
            public List<Service> getWebsiteServicesByServerId(@PathVariable("serverId") String serverId) {
                List<Service> services = new ArrayList<>();

                Service service = new Service();
                service.setId(mockedServiceId);

                ServiceType serviceType = new ServiceType();
                serviceType.setName("WEBSITE_APACHE2_PHP56_DEFAULT");

                ServiceTemplate serviceTemplate = new ServiceTemplate();
                serviceTemplate.setServiceType(serviceType);

                service.setServiceTemplate(serviceTemplate);

                services.add(service);

                return services;
            }

            @Override
            public List<Service> getDatabaseServicesByServerId(@PathVariable("serverId") String serverId) {
                List<Service> services = new ArrayList<>();

                Service service = new Service();
                service.setId(mockedDBServiceId);

                ServiceType serviceType = new ServiceType();
                serviceType.setName("DATABASE_MYSQL");

                ServiceTemplate serviceTemplate = new ServiceTemplate();
                serviceTemplate.setServiceType(serviceType);

                service.setServiceTemplate(serviceTemplate);

                services.add(service);

                return services;
            }

            @Override
            public List<Service> getServicesByServerIdAndServiceType(String serverId, String serviceType) {
                return null;
            }

            @Override
            public List<Service> getServices() {
                return null;
            }

            @Override
            public List<Service> getNginxServicesByServerId(String serverId) {
                List<Service> services = new ArrayList<>();

                Service service = new Service();
                service.setId(mockedNginxServiceId);

                ServiceType serviceType = new ServiceType();
                serviceType.setName("STAFF_NGINX");

                ServiceTemplate serviceTemplate = new ServiceTemplate();
                serviceTemplate.setServiceType(serviceType);

                service.setServiceTemplate(serviceTemplate);

                services.add(service);

                return services;
            }

            @Override
            public List<Service> getServicesOnlyIdAndName() {
                return null;
            }

            @Override
            public List<Server> getServersOnlyIdAndName() {
                return null;
            }
        };
    }

}
