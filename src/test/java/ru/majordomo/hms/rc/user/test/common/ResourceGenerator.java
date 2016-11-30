package ru.majordomo.hms.rc.user.test.common;

import com.jcraft.jsch.JSchException;

import org.bson.types.ObjectId;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.rc.user.common.SSHKeyManager;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.LegalEntity;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.rc.user.resources.CharSet.UTF8;
import static ru.majordomo.hms.rc.user.resources.DBType.MYSQL;
import static ru.majordomo.hms.rc.user.resources.DBType.POSTGRES;
import static ru.majordomo.hms.rc.user.resources.DNSResourceRecordClass.*;
import static ru.majordomo.hms.rc.user.resources.DNSResourceRecordType.*;
import static ru.majordomo.hms.rc.user.resources.DNSResourceRecordType.MX;
import static ru.majordomo.hms.rc.user.resources.DomainRegistrar.NETHOUSE;
import static ru.majordomo.hms.rc.user.resources.DomainState.DELEGATED;
import static ru.majordomo.hms.rc.user.resources.DomainState.VERIFIED;

public class ResourceGenerator {
    public static List<Person> generateBatchOfPerson() {
        List<Person> batchOfPersons = new ArrayList<>();

        Person parovozov = new Person();
        parovozov.setId(ObjectId.get().toString());
        parovozov.setAccountId(ObjectId.get().toString());
        parovozov.setName("Паровозов Аркадий Локомотивович");
        parovozov.addEmailAddress("arkady@parovozov.ru");
        parovozov.addEmailAddress("parovozov@gmail.com");
        parovozov.addPhoneNumber("+79110000911");
        parovozov.addPhoneNumber("+79110000001");
        parovozov.setOwner(true);

        Passport passport = generatePassport();
        parovozov.setPassport(passport);

        Person hosting = new Person();
        hosting.setId(ObjectId.get().toString());
        hosting.setName("ООО Хостинг");
        hosting.setAccountId(ObjectId.get().toString());
        hosting.setId(ObjectId.get().toString());
        hosting.addEmailAddress("support@majordomo.ru");
        hosting.addEmailAddress("info@majordomo.ru");
        hosting.addPhoneNumber("+78123353545");
        hosting.addPhoneNumber("+74957272278");
        hosting.setOwner(false);

        LegalEntity hostingInfo = generateLegalEntity();

        hosting.setLegalEntity(hostingInfo);

        batchOfPersons.add(parovozov);
        batchOfPersons.add(hosting);

        return batchOfPersons;
    }

    public static LegalEntity generateLegalEntity() {
        LegalEntity legalEntity = new LegalEntity();
        legalEntity.setInn("7814522538");
        legalEntity.setOkpo("30727716");
        legalEntity.setKpp("781401001");
        legalEntity.setOgrn("781401001");
        legalEntity.setOkvedCodes("64.2, 72.20, 72.40, 72.60, 74.40");
        legalEntity.setAddress("Санкт-Петербург, Торфяная дор. 7Ф, оф. 1320");

        return legalEntity;
    }

    public static Passport generatePassport() {
        Passport passport = new Passport();
        passport.setNumber("1234567890");
        passport.setBirthday("1970-01-01");
        passport.setIssuedOrg("ОУФМС по г. Ижевск");
        passport.setIssuedDate("2005-05-20");
        passport.setMainPage("http://storage/" + ObjectId.get().toString());
        passport.setRegisterPage("http://storage/" + ObjectId.get().toString());
        passport.setAddress("Санкт-Петербург, пр. Невский, д.24, кв. 2");

        return passport;
    }
    public static List<Database> generateBatchOfDatabases() throws UnsupportedEncodingException {
        List<Database> batchOfDatabases = new ArrayList<>();
        List<DatabaseUser> batchOfDatabaseUsers = generateBatchOfDatabaseUsers();
        for (int i = 2; i <= 4; i++) {
            Database database = new Database();
            database.setAccountId(ObjectId.get().toString());
            database.setName("Тестовая база" + i);
            database.setSwitchedOn(true);
            if ((i % 2) == 0) {
                database.setType(MYSQL);
                for (DatabaseUser databaseUser : batchOfDatabaseUsers) {
                    if (databaseUser.getType() == MYSQL) {
                        database.addDatabaseUser(databaseUser);
                    }
                }
            } else {
                database.setType(POSTGRES);
                for (DatabaseUser databaseUser : batchOfDatabaseUsers) {
                    if (databaseUser.getType() == POSTGRES) {
                        database.addDatabaseUser(databaseUser);
                    }
                }
            }
            database.setQuota(100000L);
            database.setQuotaUsed(5000L);
            database.setWritable(true);
            batchOfDatabases.add(database);
        }
        return batchOfDatabases;
    }

    public static List<Domain> generateBatchOfDomains() {
        List<Domain> batchOfDOmains = new ArrayList<>();
        List<Person> batchOfPersons = generateBatchOfPerson();

        RegSpec regSpec = new RegSpec();
        regSpec.setRegistrar(NETHOUSE);
        regSpec.setCreatedAsString("2016-10-01");
        regSpec.setFreeDateAsString("2017-11-01");
        regSpec.setPaidTillAsString("2016-10-01");
        regSpec.addState(DELEGATED);
        regSpec.addState(VERIFIED);

        Domain ruDomain = new Domain();
        ruDomain.setId(ObjectId.get().toString());
        ruDomain.setAccountId(ObjectId.get().toString());
        ruDomain.setName("majordomo.ru");
        ruDomain.setSwitchedOn(true);
        ruDomain.setDnsResourceRecords(generateBatchOfDNSRecords("majordomo.ru"));
        ruDomain.setRegSpec(regSpec);
        ruDomain.setPerson(batchOfPersons.get(0));

        Domain rfDomain = new Domain();
        rfDomain.setId(ObjectId.get().toString());
        rfDomain.setAccountId(ObjectId.get().toString());
        rfDomain.setName("мажордомо.рф");
        rfDomain.setSwitchedOn(true);
        rfDomain.setPerson(batchOfPersons.get(1));
        rfDomain.setDnsResourceRecords(generateBatchOfDNSRecords("мажордомо.рф"));
        rfDomain.setRegSpec(regSpec);

        batchOfDOmains.add(ruDomain);
        batchOfDOmains.add(rfDomain);

        return batchOfDOmains;
    }

    public static List<DNSResourceRecord> generateBatchOfDNSRecords(String domainName) {
        List<DNSResourceRecord> records = new ArrayList<>();
        DNSResourceRecord aRecord = new DNSResourceRecord();
        aRecord.setOwnerName(domainName);
        aRecord.setRrClass(IN);
        aRecord.setRrType(A);
        aRecord.setData("78.108.80.1");
        aRecord.setTtl(300L);

        DNSResourceRecord mxRecord = new DNSResourceRecord();
        mxRecord.setOwnerName(domainName);
        mxRecord.setRrClass(IN);
        mxRecord.setRrType(MX);
        mxRecord.setData("mxs.majordomo.ru");
        mxRecord.setTtl(300L);

        records.add(aRecord);
        records.add(mxRecord);

        return records;
    }

    public static List<Mailbox> generateBatchOfMailboxes() {
        List<Mailbox> batchOfMailboxes = new ArrayList<>();
        List<Domain> batchOfDomains = generateBatchOfDomains();

        for (int i = 0; i < 2; i++) {
            Mailbox mailbox = new Mailbox();
            mailbox.setAccountId(ObjectId.get().toString());
            mailbox.setName("box" + i + "@majordomo.ru");
            mailbox.setSwitchedOn(true);
            Arrays.asList("bad@address.ru", "spam@gmail.com").forEach(mailbox::addToBlackList);
            Arrays.asList("good@yandex.ru", "good@mail.ru").forEach(mailbox::addToWhiteList);
            mailbox.setDomain(batchOfDomains.get(i));
            mailbox.setAntiSpamEnabled(true);
            mailbox.setServerId(ObjectId.get().toString());
            mailbox.setQuota(1000000L);
            mailbox.setQuotaUsed(5000L);
            mailbox.setWritable(true);

            batchOfMailboxes.add(mailbox);
        }

        return batchOfMailboxes;
    }

    public static List<UnixAccount> generateBatchOfUnixAccounts() throws JSchException {
        List<UnixAccount> batchOfUnixAccounts = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int nameNumPart = 134035 + i;
            UnixAccount unixAccount = new UnixAccount();
            unixAccount.setAccountId(ObjectId.get().toString());
            unixAccount.setId(ObjectId.get().toString());
            unixAccount.setName("u" + nameNumPart);
            unixAccount.setSwitchedOn(true);
            unixAccount.setHomeDir("/home/u" + nameNumPart);
            unixAccount.setUid(2005 + i);
            unixAccount.setServerId(ObjectId.get().toString());
            unixAccount.setQuota(10485760L);
            unixAccount.setQuotaUsed(1048576L);
            unixAccount.setWritable(true);
            unixAccount.setCrontab(generateBatchOfCronTasks());
            unixAccount.setKeyPair(SSHKeyManager.generateKeyPair());

            batchOfUnixAccounts.add(unixAccount);
        }

        return batchOfUnixAccounts;
    }

    public static List<WebSite> generateBatchOfWebsites() throws JSchException {
        List<WebSite> batchOfWebsites = new ArrayList<>();
        List<Domain> batchOfDomains = generateBatchOfDomains();
        List<UnixAccount> batchOfUnixAccounts = generateBatchOfUnixAccounts();

        for (int i = 0; i < 2; i++) {
            WebSite webSite = new WebSite();
            if (i == 0) {
                webSite.setName("Сайт компании");
                webSite.setDocumentRoot("majordomo.ru/www");
            } else {
                webSite.setName("Блог");
                webSite.setDocumentRoot("yandex.ru/www");
            }

            webSite.setAccountId(ObjectId.get().toString());
            webSite.setSwitchedOn(true);
            webSite.setUnixAccount(batchOfUnixAccounts.get(i));
            webSite.addDomain(batchOfDomains.get(i));
            webSite.setCharSet(UTF8);
            webSite.setSsiEnabled(true);
            webSite.setSsiFileExtensions(Arrays.asList("shtml", "shtm"));
            webSite.setCgiEnabled(true);
            webSite.setCgiFileExtensions(Arrays.asList("cgi", "pl"));
            webSite.setScriptAlias("cgi-bin");
            webSite.setDdosProtection(false);
            webSite.setAutoSubDomain(true);
            webSite.setAccessByOldHttpVersion(false);
            webSite.setStaticFileExtensions(Arrays.asList("css", "htm", "png"));
            webSite.setCustomUserConf("php_flag error_repoting on");
            webSite.setIndexFileList(Arrays.asList("index.php", "index.html"));
            webSite.setAccessLogEnabled(true);
            webSite.setErrorLogEnabled(false);
            webSite.setServiceId(ObjectId.get().toString());

            batchOfWebsites.add(webSite);
        }

        return batchOfWebsites;
    }

    public static List<WebSite> generateBatchOfCertainWebsites(String accountId, String serviceId, String unixAccountId, List<String> domainIds) {
        List<WebSite> batchOfWebsites = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            WebSite webSite = new WebSite();
            if (i == 0) {
                webSite.setName("Сайт" + i);
                webSite.setDocumentRoot("majordomo.ru/www");
            } else {
                webSite.setName("Блог");
                webSite.setDocumentRoot("yandex.ru/www");
            }

            webSite.setAccountId(accountId);
            webSite.setSwitchedOn(true);
            webSite.setUnixAccountId(unixAccountId);
            webSite.setDomainIds(domainIds);
            webSite.setCharSet(UTF8);
            webSite.setSsiEnabled(true);
            webSite.setSsiFileExtensions(Arrays.asList("shtml", "shtm"));
            webSite.setCgiEnabled(false);
            webSite.setScriptAlias("cgi-bin");
            webSite.setDdosProtection(false);
            webSite.setAutoSubDomain(true);
            webSite.setAccessByOldHttpVersion(false);
            webSite.setStaticFileExtensions(Arrays.asList("css", "htm", "png"));
            webSite.setCustomUserConf("php_flag error_repoting on");
            webSite.setIndexFileList(Arrays.asList("index.php", "index.html"));
            webSite.setAccessLogEnabled(true);
            webSite.setErrorLogEnabled(false);
            webSite.setServiceId(serviceId);
            webSite.setId(ObjectId.get().toString());

            batchOfWebsites.add(webSite);
        }

        return batchOfWebsites;
    }

    public static List<CronTask> generateBatchOfCronTasks() {
        List<CronTask> crontab = new ArrayList<>();
        String atPattern = "*/${MINUTES} * * * *";
        for (Integer i = 10; i < 15; i++) {
            CronTask cronTask = new CronTask();
            cronTask.setName("Напоминатор нумеро " + i);
            cronTask.setSwitchedOn(true);
            String execTime = atPattern.replaceAll("\\$\\{MINUTES\\}", i.toString());
            cronTask.setExecTime(execTime);
            cronTask.setCommand("/bin/remind_me_every_" + i + "_minutes");

            crontab.add(cronTask);
        }

        return crontab;
    }

    public static List<FTPUser> generateBatchOfFTPUsers() throws UnsupportedEncodingException, JSchException {
        List<FTPUser> ftpUsers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            FTPUser ftpUser = new FTPUser();
            ftpUser.setAccountId(ObjectId.get().toString());
            ftpUser.setName("f13403" + i);
            ftpUser.setPasswordHashByPlainPassword("123456" + i);
            ftpUser.setHomeDir("/home/u13403" + i + "/majordomoru/" + i);
            ftpUser.setSwitchedOn(true);
            ftpUser.setUnixAccount(generateBatchOfUnixAccounts().get(0));
            ftpUsers.add(ftpUser);
        }

        return ftpUsers;
    }

    public static List<DatabaseUser> generateBatchOfDatabaseUsers() throws UnsupportedEncodingException {
        List<DatabaseUser> batchOfDatabaseUsers = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            DatabaseUser databaseUser = new DatabaseUser();
            databaseUser.setAccountId(ObjectId.get().toString());
            databaseUser.setServiceId("583300c5a94c541d14d58c85");
            databaseUser.setId(ObjectId.get().toString());
            databaseUser.setType(POSTGRES);
            databaseUser.setPasswordHashByPlainPassword("123456" + i);
            databaseUser.setAllowedIpsAsString(Collections.emptyList());
            databaseUser.setName("u10000" + i);
            databaseUser.setSwitchedOn(true);

            batchOfDatabaseUsers.add(databaseUser);
        }

        batchOfDatabaseUsers.get(1).setType(MYSQL);
        batchOfDatabaseUsers.get(1).setPasswordHashByPlainPassword("1234561");
        batchOfDatabaseUsers.get(2).setAccountId(batchOfDatabaseUsers.get(1).getAccountId());
        return batchOfDatabaseUsers;
    }
}
