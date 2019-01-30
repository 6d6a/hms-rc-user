package ru.majordomo.hms.rc.user.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.IDN;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.DTO.FieldWithStringsContainer;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;

import static ru.majordomo.hms.rc.user.common.Constants.API_NAME_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.DOMAINS_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.DOMAIN_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.EMAIL;
import static ru.majordomo.hms.rc.user.common.Constants.FROM_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.PARAMETRS_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.PRIORITY_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.REPLY_TO_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.SEND_ONLY_TO_ACTIVE_KEY;
import static ru.majordomo.hms.rc.user.common.Constants.TYPE_KEY;

@Service
@Slf4j
public class AlienDomainsSearcher {
    private final String tmpDir = System.getProperty("java.io.tmpdir") + "/";
    private final String domainListFileArchivePath = tmpDir + "Domainlist.gz";
    private final String domainListAllFilePath = tmpDir + "DomainlistAll.txt";

    private final GovernorOfDomain governorOfDomain;
    private final PmFeignClient pmFeignClient;

    public AlienDomainsSearcher(
            GovernorOfDomain governorOfDomain,
            PmFeignClient pmFeignClient
    ) {
        this.governorOfDomain = governorOfDomain;
        this.pmFeignClient = pmFeignClient;
    }

    public void search() {
        prepareSearch();

        File domainListAllFile = new File(domainListAllFilePath);

        if (!domainListAllFile.exists()) {
            log.error("domainListAllFile dos not exist");
            return;
        }

        List<FieldWithStringsContainer> accountsWithAlienDomains = governorOfDomain.findAccountsWithAlienDomainNames();
        Map<String, Map<String, String>> mapOfDomains;
        Map<String, List<Map<String, String>>> mapOfAccounts = new HashMap<>();

        mapOfDomains = accountsWithAlienDomains
                .stream()
                .flatMap(fieldWithStringsContainer -> fieldWithStringsContainer.getStrings().stream().map(domainName -> {
                            Map<String, String> accMap = new HashMap<>();
                            accMap.put("accountId", fieldWithStringsContainer.getField());
                            accMap.put("domainName", IDN.toUnicode(domainName).toLowerCase());

                            return accMap;
                        }
                ))
                .collect(Collectors.toMap(p -> p.get("domainName"), p -> p));

        try {
            LineIterator it = FileUtils.lineIterator(domainListAllFile, "UTF-8");
            try {
                while (it.hasNext()) {
                    String line = it.nextLine();
                    String[] strings = line.split("\t");
                    if (strings.length >= 2) {
                        String domain = IDN.toUnicode(strings[0]).toLowerCase();
                        String registrator = strings[1];

                        if (mapOfDomains.containsKey(domain)) {
                            Map<String, String> domainMap = mapOfDomains.get(domain);
                            domainMap.put("registrator", registrator);
                            mapOfDomains.put(domain, domainMap);
                        }
                    }
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mapOfDomains.forEach((key, value) -> {
            String accountId = value.get("accountId");
            if (mapOfAccounts.containsKey(accountId)) {
                List<Map<String, String>> accountDomains = mapOfAccounts.get(accountId);
                accountDomains.add(value);
            } else {
                List<Map<String, String>> accountDomains = new ArrayList<>();
                accountDomains.add(value);
                mapOfAccounts.put(accountId, accountDomains);
            }
        });

        AtomicInteger emailsSent = new AtomicInteger();
        AtomicInteger clientsChecked = new AtomicInteger();
        AtomicInteger domainsChecked = new AtomicInteger();
        AtomicInteger rucenterEmailsSent = new AtomicInteger();

        mapOfAccounts.forEach((accountId, listOfAlienDomains) -> {
            List<String> alienDomains = new ArrayList<>();
            List<String> ruCenterDomains = new ArrayList<>();

            clientsChecked.incrementAndGet();
            listOfAlienDomains.forEach(domain -> {
                domainsChecked.incrementAndGet();

                String registrator = domain.get("registrator");
                String domainName = domain.get("domainName");

                if (registrator != null
                        && !registrator.equals("")
                        && !registrator.equals("NETHOUSE-RU")
                        && !registrator.equals("NETHOUSE-RF")
                        && !registrator.equals("NETHOUSE-SU")
                ) {
                    log.info("match: " + domainName + " " + registrator);

                    if (registrator.equals("RU-CENTER-RU")
                            || registrator.equals("RUCENTER-RF")
                            || registrator.equals("RUCENTER-SU")
                    ) {
                        if (ruCenterDomains.size() <= 9) {
                            ruCenterDomains.add(domainName);
                        }

                    } else {
                        if (alienDomains.size() <= 9) {
                            alienDomains.add(domainName);
                        }
                    }
                }
            });

            if (ruCenterDomains.size() == 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAIN_KEY, ruCenterDomains.get(0));

                sendEmail(accountId, "MajordomoVHRuCenterDomains", params);

                rucenterEmailsSent.incrementAndGet();
            }

            if (alienDomains.size() == 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAIN_KEY, alienDomains.get(0));

                sendEmail(accountId, "MajordomoVHAlienDomains", params);

                emailsSent.incrementAndGet();
            }

            if (ruCenterDomains.size() > 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAINS_KEY, String.join("<br/>", ruCenterDomains));

                sendEmail(accountId, "MajordomoVHRuCenterManyDomains", params);

                rucenterEmailsSent.incrementAndGet();
            }

            if (alienDomains.size() > 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAINS_KEY, String.join("<br/>", alienDomains));

                sendEmail(accountId, "MajordomoVHAlienManyDomains", params);

                emailsSent.incrementAndGet();
            }

        });

        log.info(
                "Emails sent: " + emailsSent + "'. " +
                        "Domains checked: '" + domainsChecked + "'. " +
                        "Ru-center emails sent: '" + rucenterEmailsSent + "'. " +
                        "Clients checked: '" + clientsChecked + "'."
        );
    }

    private void prepareSearch() {
        String templateUrl = "https://uapweb.tcinet.ru:8092/getstat_su?login=NETHOUSE-WEB-#DOMAIN#&passwd=#PASSWORD#&file=Domainlist#DATE#.gz";

        Map<String, String> zonePasswords = new HashMap<>();

        zonePasswords.put("RU", "snwphvmd");
        zonePasswords.put("SU", "tnqpzdkh");
        zonePasswords.put("RF", "snwphvmd");

        File file = new File(domainListAllFilePath);

        if (file.exists()) {
            file.delete();
        }

        zonePasswords.forEach((zone, password) -> {
            String fileUrl = templateUrl
                    .replace("#DOMAIN#", zone)
                    .replace("#PASSWORD#", password)
                    .replace("#DATE#", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            try {
                URL url = new URL(fileUrl);
                ReadableByteChannel domainListFileChannel = Channels.newChannel(url.openStream());

                FileOutputStream domainListFileArchiveOutputStream = new FileOutputStream(domainListFileArchivePath);
                FileChannel domainListFileArchiveChannel = domainListFileArchiveOutputStream.getChannel();

                domainListFileArchiveChannel.transferFrom(domainListFileChannel, 0, Long.MAX_VALUE);

                FileInputStream domainListFileArchiveInputStream = new FileInputStream(domainListFileArchivePath);

                GZIPInputStream gis = new GZIPInputStream(domainListFileArchiveInputStream);

                FileChannel domainListAllFileChannel = new FileOutputStream(domainListAllFilePath, true).getChannel();
                domainListAllFileChannel.transferFrom(Channels.newChannel(gis), domainListAllFileChannel.size(), Long.MAX_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendEmail(String accountId, String apiName, Map<String, String> params) {
        ServiceMessage message = new ServiceMessage();

        message.setAccountId(accountId);

        message.addParam(PARAMETRS_KEY, params);
        message.addParam(API_NAME_KEY, apiName);
        message.addParam(TYPE_KEY, EMAIL);
        message.addParam(PRIORITY_KEY, 7);
        message.addParam(SEND_ONLY_TO_ACTIVE_KEY, true);

        pmFeignClient.sendNotificationToClient(message);
    }
}
