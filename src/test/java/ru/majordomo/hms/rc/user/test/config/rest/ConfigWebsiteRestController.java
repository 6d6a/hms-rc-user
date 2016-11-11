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
import ru.majordomo.hms.rc.user.api.http.UnixAccountRESTController;
import ru.majordomo.hms.rc.user.api.http.WebSiteRESTController;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrar;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebMvc
@EnableMongoRepositories("ru.majordomo.hms.rc.user.repositories")
public class ConfigWebsiteRestController extends AbstractMongoConfiguration {
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
    public WebSiteRESTController webSiteRESTController() {
        return new WebSiteRESTController();
    }

    @Bean
    public GovernorOfUnixAccount governorOfUnixAccount() {
        return new GovernorOfUnixAccount();
    }

    @Bean
    public GovernorOfWebSite governorOfWebSite() {
        return new GovernorOfWebSite();
    }

    @Bean
    public GovernorOfDomain governorOfDomain() {
        return new GovernorOfDomain();
    }

    @Bean
    public GovernorOfPerson governorOfPerson() {
        return new GovernorOfPerson();
    }

    @Bean
    public Cleaner cleaner() {
        return new Cleaner();
    }

    @Bean
    public DomainRegistrar domainRegistrar() {
        return new DomainRegistrar() {
            @Override
            public void register(Domain domain) {

            }

            @Override
            public void renew(Domain domain) {

            }
        };
    }

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
