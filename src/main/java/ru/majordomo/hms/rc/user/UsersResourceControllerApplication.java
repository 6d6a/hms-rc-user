package ru.majordomo.hms.rc.user;

import org.apache.commons.validator.routines.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;

import java.util.Arrays;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients(basePackages = {"ru.majordomo.hms.rc.user.api.interfaces"})
@EnableMongoAuditing
@EnableConfigurationProperties(DefaultWebSiteSettings.class)
public class UsersResourceControllerApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UsersResourceControllerApplication.class);
    private static final String[] tldList = {"llc"};
    static {
        Arrays.sort(tldList);
        DomainValidator.updateTLDOverride(DomainValidator.ArrayType.GENERIC_PLUS, tldList);
    }

    public static void main(String[] args) {
        SpringApplication.run(UsersResourceControllerApplication.class, args);
    }

    public void run(String... args) {
        String processOption = "--process";
        StringBuilder sb = new StringBuilder();
        for (String option : args) {
            sb.append(" ").append(option);
            if (option.equals(processOption)) {
                //Do some shit
            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched UserResourceController with following options: %s", sb.toString()));
    }
}
