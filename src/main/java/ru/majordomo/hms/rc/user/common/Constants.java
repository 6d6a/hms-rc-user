package ru.majordomo.hms.rc.user.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final String PM = "pm";
    public static final String TE = "te";
    public static final String RC_USER = "rc.user";
    public static final String LETSENCRYPT = "letsencrypt";

    public static class Exchanges {
        public static final String ACCOUNT_CREATE = "account.create";
        public static final String ACCOUNT_UPDATE = "account.update";
        public static final String ACCOUNT_DELETE = "account.delete";

        public static final String ACCOUNT_HISTORY = "account-history";

        public static final String DATABASE_CREATE = "database.create";
        public static final String DATABASE_UPDATE = "database.update";
        public static final String DATABASE_DELETE = "database.delete";

        public static final String DATABASE_USER_CREATE = "database-user.create";
        public static final String DATABASE_USER_UPDATE = "database-user.update";
        public static final String DATABASE_USER_DELETE = "database-user.delete";

        public static final String DNS_RECORD_CREATE = "dns-record.create";
        public static final String DNS_RECORD_UPDATE = "dns-record.update";
        public static final String DNS_RECORD_DELETE = "dns-record.delete";

        public static final String DOMAIN_CREATE = "domain.create";
        public static final String DOMAIN_UPDATE = "domain.update";
        public static final String DOMAIN_DELETE = "domain.delete";

        public static final String FTP_USER_CREATE = "ftp-user.create";
        public static final String FTP_USER_UPDATE = "ftp-user.update";
        public static final String FTP_USER_DELETE = "ftp-user.delete";

        public static final String MAILBOX_CREATE = "mailbox.create";
        public static final String MAILBOX_UPDATE = "mailbox.update";
        public static final String MAILBOX_DELETE = "mailbox.delete";

        public static final String PAYMENT_CREATE = "payment.create";

        public static final String PERSON_CREATE = "person.create";
        public static final String PERSON_UPDATE = "person.update";
        public static final String PERSON_DELETE = "person.delete";

        public static final String RESOURCE_ARCHIVE_CREATE = "resource-archive.create";
        public static final String RESOURCE_ARCHIVE_UPDATE = "resource-archive.update";
        public static final String RESOURCE_ARCHIVE_DELETE = "resource-archive.delete";

        public static final String SSL_CERTIFICATE_CREATE = "ssl-certificate.create";
        public static final String SSL_CERTIFICATE_UPDATE = "ssl-certificate.update";
        public static final String SSL_CERTIFICATE_DELETE = "ssl-certificate.delete";

        public static final String UNIX_ACCOUNT_CREATE = "unix-account.create";
        public static final String UNIX_ACCOUNT_UPDATE = "unix-account.update";
        public static final String UNIX_ACCOUNT_DELETE = "unix-account.delete";

        public static final String WEBSITE_CREATE = "website.create";
        public static final String WEBSITE_UPDATE = "website.update";
        public static final String WEBSITE_DELETE = "website.delete";

        public static Set<String> ALL_EXCHANGES;

        static {
            ALL_EXCHANGES = new HashSet<>(Arrays.asList(
                    ACCOUNT_CREATE,
                    ACCOUNT_UPDATE,
                    ACCOUNT_DELETE,
                    ACCOUNT_HISTORY,
                    DATABASE_CREATE,
                    DATABASE_UPDATE,
                    DATABASE_DELETE,
                    DATABASE_USER_CREATE,
                    DATABASE_USER_UPDATE,
                    DATABASE_USER_DELETE,
                    DNS_RECORD_CREATE,
                    DNS_RECORD_UPDATE,
                    DNS_RECORD_DELETE,
                    DOMAIN_CREATE,
                    DOMAIN_UPDATE,
                    DOMAIN_DELETE,
                    FTP_USER_CREATE,
                    FTP_USER_UPDATE,
                    FTP_USER_DELETE,
                    MAILBOX_CREATE,
                    MAILBOX_UPDATE,
                    MAILBOX_DELETE,
                    PAYMENT_CREATE,
                    PERSON_CREATE,
                    PERSON_UPDATE,
                    PERSON_DELETE,
                    RESOURCE_ARCHIVE_CREATE,
                    RESOURCE_ARCHIVE_UPDATE,
                    RESOURCE_ARCHIVE_DELETE,
                    SSL_CERTIFICATE_CREATE,
                    SSL_CERTIFICATE_UPDATE,
                    SSL_CERTIFICATE_DELETE,
                    UNIX_ACCOUNT_CREATE,
                    UNIX_ACCOUNT_UPDATE,
                    UNIX_ACCOUNT_DELETE,
                    WEBSITE_CREATE,
                    WEBSITE_UPDATE,
                    WEBSITE_DELETE
            ));
        }
    }
}
