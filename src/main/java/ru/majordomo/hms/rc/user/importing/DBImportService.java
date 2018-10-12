package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("import")
public class DBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DBImportService.class);

    private final DatabaseUserDBImportService databaseUserDBImportService;
    private final DatabaseDBImportService databaseDBImportService;
    private final FTPUserDBImportService ftpUserDBImportService;
    private final UnixAccountDBImportService unixAccountDBImportService;
    private final SSLCertificateDBImportService sslCertificateDBImportService;
    private final DomainDBImportService domainDBImportService;
    private final DomainSubDomainDBImportService domainSubDomainDBImportService;
    private final MailboxDBImportService mailboxDBImportService;
    private final WebSiteDBImportService webSiteDBImportService;
    private final PersonDBImportService personDBImportService;

    @Autowired
    public DBImportService(
            DatabaseUserDBImportService databaseUserDBImportService,
            DatabaseDBImportService databaseDBImportService,
            FTPUserDBImportService ftpUserDBImportService,
            UnixAccountDBImportService unixAccountDBImportService,
            SSLCertificateDBImportService sslCertificateDBImportService,
            DomainDBImportService domainDBImportService,
            DomainSubDomainDBImportService domainSubDomainDBImportService,
            MailboxDBImportService mailboxDBImportService,
            WebSiteDBImportService webSiteDBImportService,
            PersonDBImportService personDBImportService
    ) {
        this.databaseUserDBImportService = databaseUserDBImportService;
        this.databaseDBImportService = databaseDBImportService;
        this.ftpUserDBImportService = ftpUserDBImportService;
        this.unixAccountDBImportService = unixAccountDBImportService;
        this.sslCertificateDBImportService = sslCertificateDBImportService;
        this.domainDBImportService = domainDBImportService;
        this.domainSubDomainDBImportService = domainSubDomainDBImportService;
        this.mailboxDBImportService = mailboxDBImportService;
        this.webSiteDBImportService = webSiteDBImportService;
        this.personDBImportService = personDBImportService;
    }

    public boolean seedDB() {
        boolean seeded;

//        seeded = businessActionDBSeedService.seedDB();
//        logger.debug(seeded ? "businessFlow db_seeded" : "businessFlow db_not_seeded");

        return true;
    }

    public boolean importToMongo() {
        boolean imported;

//        imported = unixAccountDBImportService.importToMongo();
//        logger.debug(imported ? "unixAccount db_imported" : "unixAccount db_not_imported");

//        imported = databaseUserDBImportService.importToMongo();
//        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");
//
//        imported = databaseDBImportService.importToMongo();
//        logger.debug(imported ? "database db_imported" : "database db_not_imported");
//
//        imported = ftpUserDBImportService.importToMongo();
//        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");
//
//        imported = sslCertificateDBImportService.importToMongo();
//        logger.debug(imported ? "sslCertificate db_imported" : "sslCertificate db_not_imported");
//
//        imported = domainDBImportService.importToMongo();
//        logger.debug(imported ? "domain db_imported" : "domain db_not_imported");

//        imported = domainSubDomainDBImportService.importToMongo();
//        logger.debug(imported ? "domainSubDomain db_imported" : "domainSubDomain db_not_imported");

//        imported = mailboxDBImportService.importToMongo();
//        logger.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");
//
//        imported = webSiteDBImportService.importToMongo();
//        logger.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");
//
//        imported = personDBImportService.importToMongo();
//        logger.debug(imported ? "person db_imported" : "person db_not_imported");

        return true;
    }

    public boolean importToMongo(String accountId) {
        boolean imported;

//        imported = unixAccountDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "unixAccount db_imported" : "unixAccount db_not_imported");
//
//        imported = databaseUserDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "databaseUser db_imported" : "databaseUser db_not_imported");
//
//        imported = databaseDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "database db_imported" : "database db_not_imported");
//
//        imported = ftpUserDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "ftpUser db_imported" : "ftpUser db_not_imported");
//
//        imported = sslCertificateDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "sslCertificate db_imported" : "sslCertificate db_not_imported");
//
//        imported = domainDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "domain db_imported" : "domain db_not_imported");
//
//        imported = domainSubDomainDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "domainSubDomain db_imported" : "domainSubDomain db_not_imported");
//
//        imported = mailboxDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");
//
//        imported = webSiteDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "webSite db_imported" : "webSite db_not_imported");
//
//        imported = personDBImportService.importToMongo(accountId);
//        logger.debug(imported ? "person db_imported" : "person db_not_imported");

        return true;
    }
}
