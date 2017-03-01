package ru.majordomo.hms.rc.user.test.config.amqp;

import org.springframework.context.annotation.Bean;
import ru.majordomo.hms.rc.user.api.amqp.DatabaseUserAMQPController;
import ru.majordomo.hms.rc.user.api.amqp.PersonAMQPController;
import ru.majordomo.hms.rc.user.api.amqp.WebSiteAMQPController;

public class ConfigAMQPControllers {
    @Bean
    public DatabaseUserAMQPController databaseUserAMQPController() {
        return new DatabaseUserAMQPController();
    }
    @Bean
    public WebSiteAMQPController webSiteAMQPController() {
        return new WebSiteAMQPController();
    }
    @Bean
    public PersonAMQPController personAMQPController() {
        return new PersonAMQPController();
    }
}
