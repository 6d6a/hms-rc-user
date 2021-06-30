package ru.majordomo.hms.rc.user.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final String PM = "pm";
    public static final String TE = "te";
    public static final String RC_USER_ROUT = "rc.user"; //Когда мы отсылаем в rabbit
    public static final String RC_USER_APP = "rc-user"; //Когда мы получам getEventProvider
    public static final String LETSENCRYPT = "letsencrypt";
    public static final int BYTES_IN_ONE_MEBIBYTE = 1024 * 1024;
    public static final String FREE_QUOTA_KEY = "free_quota";
    public static final String NAME_KEY = "name";
    public static final String RESOURCE_KEY = "resource";
    public static final String QUOTA_KEY = "quota";
    public static final String PARAMETRS_KEY = "parametrs";
    public static final String API_NAME_KEY = "api_name";
    public static final String TYPE_KEY = "type";
    public static final String EMAIL = "EMAIL";
    public static final String SMS = "SMS";
    public static final String PRIORITY_KEY = "priority";
    public static final String FROM_KEY = "from";
    public static final String REPLY_TO_KEY = "reply_to";
    public static final String DOMAIN_KEY = "domain";
    public static final String DOMAINS_KEY = "domains";
    public static final String REGISTRATOR_KEY = "registrator";
    public static final String SEND_ONLY_TO_ACTIVE_KEY = "send_only_to_active";
    public static final String MAIL_ENVELOPE_FROM = "noreply@";
    public static final String SUCCESS_KEY = "success";
    /** {@code @Nullable Boolean}, null - не менять агрегатор */
    public static final String IS_AGGREGATOR_KEY = "isAggregator";
    public static final String DOMAIN_ID_KEY = "domainId";
    public static final String RESOURCE_ID_KEY = "resourceId";

    public static final String MAJORDOMO_SITE_NAME = "majordomo.ru";

    public static class Exchanges {
        public static class Resource {
            public static final String ACCOUNT = "account";
            public static final String DATABASE = "database";
            public static final String DATABASE_USER = "database-user";
            public static final String DNS_RECORD = "dns-record";
            public static final String DOMAIN = "domain";
            public static final String FTP_USER = "ftp-user";
            public static final String MAILBOX = "mailbox";
            public static final String PERSON = "person";
            public static final String UNIX_ACCOUNT = "unix-account";
            public static final String WEBSITE = "website";
            public static final String REDIRECT = "redirect";
            public static final String SSL_CERTIFICATE = "ssl-certificate";
            public static final String RESOURCE_ARCHIVE = "resource-archive";
            public static final String PAYMENT = "payment";
        }

        public static class Command {
            public static final String CREATE = "create";
            public static final String UPDATE = "update";
            public static final String DELETE = "delete";
            public static final String QUOTA = "quota";
        }

        public static final String ACCOUNT_CREATE = Resource.ACCOUNT + "." + Command.CREATE;
        public static final String ACCOUNT_UPDATE = Resource.ACCOUNT + "." + Command.UPDATE;
        public static final String ACCOUNT_DELETE = Resource.ACCOUNT + "." + Command.DELETE;

        public static final String ACCOUNT_HISTORY = "account-history";

        public static final String DATABASE_CREATE = Resource.DATABASE + "." + Command.CREATE;
        public static final String DATABASE_UPDATE = Resource.DATABASE + "." + Command.UPDATE;
        public static final String DATABASE_DELETE = Resource.DATABASE + "." + Command.DELETE;

        public static final String DATABASE_USER_CREATE = Resource.DATABASE_USER + "." + Command.CREATE;
        public static final String DATABASE_USER_UPDATE = Resource.DATABASE_USER + "." + Command.UPDATE;
        public static final String DATABASE_USER_DELETE = Resource.DATABASE_USER + "." + Command.DELETE;

        public static final String DNS_RECORD_CREATE = Resource.DNS_RECORD + "." + Command.CREATE;
        public static final String DNS_RECORD_UPDATE = Resource.DNS_RECORD + "." + Command.UPDATE;
        public static final String DNS_RECORD_DELETE = Resource.DNS_RECORD + "." + Command.DELETE;

        public static final String DOMAIN_CREATE = Resource.DOMAIN + "." + Command.CREATE;
        public static final String DOMAIN_UPDATE = Resource.DOMAIN + "." + Command.UPDATE;
        public static final String DOMAIN_DELETE = Resource.DOMAIN + "." + Command.DELETE;

        public static final String FTP_USER_CREATE = Resource.FTP_USER + "." + Command.CREATE;
        public static final String FTP_USER_UPDATE = Resource.FTP_USER + "." + Command.UPDATE;
        public static final String FTP_USER_DELETE = Resource.FTP_USER + "." + Command.DELETE;

        public static final String MAILBOX_CREATE = Resource.MAILBOX + "." + Command.CREATE;
        public static final String MAILBOX_UPDATE = Resource.MAILBOX + "." + Command.UPDATE;
        public static final String MAILBOX_DELETE = Resource.MAILBOX + "." + Command.DELETE;

        public static final String PAYMENT_CREATE = Resource.PAYMENT + "." + Command.CREATE;

        public static final String PERSON_CREATE = Resource.PERSON + "." + Command.CREATE;
        public static final String PERSON_UPDATE = Resource.PERSON + "." + Command.UPDATE;
        public static final String PERSON_DELETE = Resource.PERSON + "." + Command.DELETE;

        public static final String RESOURCE_ARCHIVE_CREATE = Resource.RESOURCE_ARCHIVE + "." + Command.CREATE;
        public static final String RESOURCE_ARCHIVE_UPDATE = Resource.RESOURCE_ARCHIVE + "." + Command.UPDATE;
        public static final String RESOURCE_ARCHIVE_DELETE = Resource.RESOURCE_ARCHIVE + "." + Command.DELETE;

        public static final String SSL_CERTIFICATE_CREATE = Resource.SSL_CERTIFICATE + "." + Command.CREATE;
        public static final String SSL_CERTIFICATE_UPDATE = Resource.SSL_CERTIFICATE + "." + Command.UPDATE;
        public static final String SSL_CERTIFICATE_DELETE = Resource.SSL_CERTIFICATE + "." + Command.DELETE;

        public static final String UNIX_ACCOUNT_CREATE = Resource.UNIX_ACCOUNT + "." + Command.CREATE;
        public static final String UNIX_ACCOUNT_UPDATE = Resource.UNIX_ACCOUNT + "." + Command.UPDATE;
        public static final String UNIX_ACCOUNT_DELETE = Resource.UNIX_ACCOUNT + "." + Command.DELETE;

        public static final String WEBSITE_CREATE = Resource.WEBSITE + "." + Command.CREATE;
        public static final String WEBSITE_UPDATE = Resource.WEBSITE + "." + Command.UPDATE;
        public static final String WEBSITE_DELETE = Resource.WEBSITE + "." + Command.DELETE;

        public static final String REDIRECT_CREATE = Resource.REDIRECT + "." + Command.CREATE;
        public static final String REDIRECT_UPDATE = Resource.REDIRECT + "." + Command.UPDATE;
        public static final String REDIRECT_DELETE = Resource.REDIRECT + "." + Command.DELETE;

        public static final String UNIX_ACCOUNT_QUOTA = Resource.UNIX_ACCOUNT + "." + Command.QUOTA;
        public static final String DATABASE_QUOTA = Resource.DATABASE + "." + Command.QUOTA;
        public static final String MAILBOX_QUOTA = Resource.MAILBOX + "." + Command.QUOTA;

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
                    WEBSITE_DELETE,
                    REDIRECT_CREATE,
                    REDIRECT_UPDATE,
                    REDIRECT_DELETE,
                    UNIX_ACCOUNT_QUOTA,
                    DATABASE_QUOTA,
                    MAILBOX_QUOTA
            ));
        }
    }
}
