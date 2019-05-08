package ru.majordomo.hms.rc.user.test.config.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ru.majordomo.hms.rc.user.api.http.*;
import ru.majordomo.hms.rc.user.service.stat.Aggregator;

@Configuration
@EnableWebMvc
public class ConfigRestControllers {
    @Bean
    public DatabaseRestController databaseRestController() {
        return new DatabaseRestController();
    }

    @Bean
    public DatabaseUserRestController databaseUserRestController() {
        return new DatabaseUserRestController();
    }

    @Bean
    public DomainRestController domainRestController() {
        return new DomainRestController();
    }

    @Bean
    public FTPUserRestController ftpUserRestController() {
        return new FTPUserRestController();
    }

    @Bean
    public MailboxRestController mailboxRestController() {
        return new MailboxRestController();
    }

    @Bean
    public PersonRestController personRestController() {
        return new PersonRestController();
    }

    @Bean
    public UnixAccountRESTController unixAccountRESTController() {
        return new UnixAccountRESTController();
    }

    @Bean
    public WebSiteRESTController webSiteRESTController() {
        return new WebSiteRESTController();
    }

    @Bean
    Aggregator aggregator() {
        return new Aggregator(null, null);
    }
}
