package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DBImportService.class);

    private final DatabaseUserDBImportService databaseUserDBImportService;
    private final DatabaseDBImportService databaseDBImportService;
    private final FTPUserDBImportService ftpUserDBImportService;
    private final UnixAccountDBImportService unixAccountDBImportService;
    private final SSLCertificateDBImportService sslCertificateDBImportService;
    private final DomainDBImportService domainDBImportService;
    private final MailboxDBImportService mailboxDBImportService;

    @Autowired
    public DBImportService(
            DatabaseUserDBImportService databaseUserDBImportService,
            DatabaseDBImportService databaseDBImportService,
            FTPUserDBImportService ftpUserDBImportService,
            UnixAccountDBImportService unixAccountDBImportService,
            SSLCertificateDBImportService sslCertificateDBImportService,
            DomainDBImportService domainDBImportService,
            MailboxDBImportService mailboxDBImportService) {
        this.databaseUserDBImportService = databaseUserDBImportService;
        this.databaseDBImportService = databaseDBImportService;
        this.ftpUserDBImportService = ftpUserDBImportService;
        this.unixAccountDBImportService = unixAccountDBImportService;
        this.sslCertificateDBImportService = sslCertificateDBImportService;
        this.domainDBImportService = domainDBImportService;
        this.mailboxDBImportService = mailboxDBImportService;
    }

    public boolean seedDB() {
        boolean seeded;

//        seeded = businessActionDBSeedService.seedDB();
//        logger.debug(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");

        return true;
    }

    public boolean importToMongo() {
        boolean imported;

        imported = unixAccountDBImportService.importToMongo();
        logger.debug(imported ? "unixAccount db_imported" : "unixAccount db_not_imported");

        imported = databaseUserDBImportService.importToMongo();
        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");

        imported = databaseDBImportService.importToMongo();
        logger.debug(imported ? "database db_imported" : "database db_not_imported");

        imported = ftpUserDBImportService.importToMongo();
        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");

        imported = sslCertificateDBImportService.importToMongo();
        logger.debug(imported ? "sslCertificate db_imported" : "sslCertificate db_not_imported");

        imported = domainDBImportService.importToMongo();
        logger.debug(imported ? "domain db_imported" : "domain db_not_imported");

        imported = mailboxDBImportService.importToMongo();
        logger.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");

        return true;
    }

    public boolean importToMongo(String accountId) {
        boolean imported;

//        imported = unixAccountDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "unixAccount db_imported" : "unixAccount db_not_imported");

//        imported = databaseUserDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");

//        imported = databaseDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "database db_imported" : "database db_not_imported");

//        imported = ftpUserDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");

//        imported = sslCertificateDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "sslCertificate db_imported" : "sslCertificate db_not_imported");

//        imported = domainDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "domain db_imported" : "domain db_not_imported");

        imported = mailboxDBImportService.importToMongo(accountId);
        logger.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");

        return true;
    }
}
