package ru.majordomo.hms.rc.user.service;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.DTO.FieldWithStringsContainer;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;

import static ru.majordomo.hms.rc.user.common.Constants.*;

@Service
@Slf4j
public class AlienDomainsSearcher {
    private final String tmpDir = System.getProperty("java.io.tmpdir") + "/";
    private final String domainListFileArchive = tmpDir + "Domainlist.gz";
    private final String domainListAllFile = tmpDir + "DomainlistAll.txt";

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
        List<FieldWithStringsContainer> accountsWithAlienDomains = governorOfDomain.findAccountsWithAlienDomainNames();

        AtomicInteger emailsSent = new AtomicInteger();
        AtomicInteger clientsChecked = new AtomicInteger();
        AtomicInteger domainsChecked = new AtomicInteger();
        AtomicInteger rucenterEmailsSent = new AtomicInteger();

        accountsWithAlienDomains.forEach(accountWithAlienDomains -> {
            List<String> alienDomains = new ArrayList<>();
            List<String> ruCenterDomains = new ArrayList<>();

            clientsChecked.incrementAndGet();
            accountWithAlienDomains.getStrings().forEach(domain -> {
                domainsChecked.incrementAndGet();

                Pattern pattern = Pattern.compile("^" + IDN.toASCII(domain).toUpperCase() + "\t([A-Z0-9-]+)\t.*");
                Matcher matcher = pattern.matcher("");

                try (Stream<String> lines = Files.lines(Paths.get(domainListAllFile))) {
                    lines.map(matcher::reset)
                            .filter(Matcher::matches)
                            .findFirst()
                            .ifPresent(m -> {
                                String registrator = m.group(1);

                                if (!registrator.equals("")
                                        && !registrator.equals("NETHOUSE-RU")
                                        && !registrator.equals("NETHOUSE-RF")
                                        && !registrator.equals("NETHOUSE-SU")
                                ) {
                                    log.info("match: " + domain + " " + m.group(1));

                                    if (registrator.equals("RU-CENTER-RU")
                                            || registrator.equals("RUCENTER-RF")
                                            || registrator.equals("RUCENTER-SU")
                                    ) {
                                        if (ruCenterDomains.size() <= 9) {
                                            ruCenterDomains.add(domain);
                                        }

                                    } else {
                                        if (alienDomains.size() <= 9) {
                                            alienDomains.add(domain);
                                        }
                                    }
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            if (ruCenterDomains.size() == 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAIN_KEY, ruCenterDomains.get(0));

                sendEmail(accountWithAlienDomains.getField(), "MajordomoVHRuCenterDomains", params);

                rucenterEmailsSent.incrementAndGet();
            }

            if (alienDomains.size() == 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAIN_KEY, alienDomains.get(0));

                sendEmail(accountWithAlienDomains.getField(), "MajordomoVHAlienDomains", params);

                emailsSent.incrementAndGet();
            }

            if (ruCenterDomains.size() > 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAINS_KEY, String.join("<br/>", ruCenterDomains));

                sendEmail(accountWithAlienDomains.getField(), "MajordomoVHRuCenterManyDomains", params);

                rucenterEmailsSent.incrementAndGet();
            }

            if (alienDomains.size() > 1) {
                Map<String, String> params = new HashMap<>();
                params.put(FROM_KEY, "noreply@majordomo.ru");
                params.put(REPLY_TO_KEY, "domain@majordomo.ru");
                params.put(DOMAINS_KEY, String.join("<br/>", alienDomains));

                sendEmail(accountWithAlienDomains.getField(), "MajordomoVHAlienManyDomains", params);

                emailsSent.incrementAndGet();
            }

        });

        log.info(
                "Emails sent: '%s'. Domains checked: '%s'. Ru-center emails sent: '%s'. Clients checked: '%s'.",
                emailsSent,
                domainsChecked,
                rucenterEmailsSent,
                clientsChecked
        );
    }

    private void prepareSearch() {
        String templateUrl = "https://uapweb.tcinet.ru:8092/getstat_su?login=NETHOUSE-WEB-#DOMAIN#&passwd=#PASSWORD#&file=Domainlist#DATE#.gz";

        Map<String, String> zonePasswords =  new HashMap<>();

        zonePasswords.put("RU", "snwphvmd");
        zonePasswords.put("SU", "tnqpzdkh");
        zonePasswords.put("RF", "snwphvmd");

        File file = new File(domainListAllFile);

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

                FileOutputStream domainListFileArchiveOutputStream = new FileOutputStream(domainListFileArchive);
                FileChannel domainListFileArchiveChannel = domainListFileArchiveOutputStream.getChannel();

                domainListFileArchiveChannel.transferFrom(domainListFileChannel, 0, Long.MAX_VALUE);

                FileInputStream domainListFileArchiveInputStream = new FileInputStream(domainListFileArchive);

                GZIPInputStream gis = new GZIPInputStream(domainListFileArchiveInputStream);

                FileChannel domainListAllFileChannel = new FileOutputStream(domainListAllFile, true).getChannel();
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

        pmFeignClient.sendNotificationToClient(message);
    }
}
