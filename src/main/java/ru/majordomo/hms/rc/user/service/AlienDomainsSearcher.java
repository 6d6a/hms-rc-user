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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.DTO.FieldWithStringsContainer;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;

@Service
@Slf4j
public class AlienDomainsSearcher {
    private final String tmpDir = System.getProperty("java.io.tmpdir") + "/";
    private final String domainListFileArchive = tmpDir + "Domainlist.gz";
    private final String domainListAllFile = tmpDir + "DomainlistAll.txt";

    private final GovernorOfDomain governorOfDomain;

    public AlienDomainsSearcher(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    public void search() {
        prepareSearch();
        List<FieldWithStringsContainer> accountsWithAlienDomains = governorOfDomain.findAccountsWithAlienDomainNames();

//        int emailsSent = 0;
//        int clientsFound = accountsWithAlienDomains.size();
//        int domainsFound = 0;
//        int rucenterEmailsSent = 0;

        accountsWithAlienDomains.forEach(accountWithAlienDomains -> {
//            log.info("accountWithAlienDomains: " + accountWithAlienDomains);
            accountWithAlienDomains.getStrings().forEach(domain -> {
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
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
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
}
