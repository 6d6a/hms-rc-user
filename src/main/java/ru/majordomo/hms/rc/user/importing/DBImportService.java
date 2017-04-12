package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DBImportService.class);

    private final DatabaseUserDBImportService databaseUserDBImportService;
    private final FTPUserDBImportService ftpUserDBImportService;

    @Autowired
    public DBImportService(
            DatabaseUserDBImportService databaseUserDBImportService,
            FTPUserDBImportService ftpUserDBImportService
    ) {
        this.databaseUserDBImportService = databaseUserDBImportService;
        this.ftpUserDBImportService = ftpUserDBImportService;
    }

    public boolean seedDB() {
        boolean seeded;

//        seeded = businessActionDBSeedService.seedDB();
//        logger.debug(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");

        return true;
    }

    public boolean importToMongo() {
        boolean imported;

        imported = databaseUserDBImportService.importToMongo();
        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");

        imported = ftpUserDBImportService.importToMongo();
        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");

        return true;
    }

    public boolean importToMongo(String accountId) {
        boolean imported;

        imported = databaseUserDBImportService.importToMongo(accountId);
        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");

        imported = ftpUserDBImportService.importToMongo(accountId);
        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");

        return true;
    }
}
