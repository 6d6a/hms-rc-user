package ru.majordomo.hms.rc.user.test.config.amqp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.majordomo.hms.rc.user.api.amqp.WebSiteAMQPController;

@Configuration
public class ConfigWebSiteAMQPControllerTest {
    @Bean
    public WebSiteAMQPController webSiteAMQPController() {
        return new WebSiteAMQPController();
    }
}
