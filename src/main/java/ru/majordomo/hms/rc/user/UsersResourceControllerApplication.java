package ru.majordomo.hms.rc.user;

import org.apache.commons.validator.routines.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;

import java.util.Arrays;

//import ru.majordomo.hms.rc.user.importing.DBImportService;

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

//    @Autowired
//    private DBImportService dbImportService;

    public static void main(String[] args) {
        SpringApplication.run(UsersResourceControllerApplication.class, args);
    }

    public void run(String... args) {
        String dbSeedOption = "--db_seed";
        String dbImportOption = "--db_import";
        String dbImportOneAccountOption = "--db_import_one_account";
        String processOption = "--process";
        StringBuilder sb = new StringBuilder();
        for (String option : args) {
            sb.append(" ").append(option);

            if (option.equals(dbSeedOption)) {
                boolean seeded;

//                seeded = dbImportService.seedDB();
//                sb.append(" ").append(seeded ? "dbImportService db_seeded" : "dbImportService db_not_seeded");
            } else if (option.equals(dbImportOption)) {
                boolean imported;

//                imported = dbImportService.importToMongo();
//                sb.append(" ").append(imported ? "dbImportService db_imported" : "dbImportService db_not_imported");
            } else if (option.equals(dbImportOneAccountOption)) {
                boolean imported;

//                imported = dbImportService.importToMongo("100800");
//                sb.append(" ").append(imported ? "dbImportService db_imported" : "dbImportService db_not_imported");
            } else if (option.equals(processOption)) {
                //Do some shit
            }
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched UserResourceController with following options: %s", sb.toString()));
    }
}
