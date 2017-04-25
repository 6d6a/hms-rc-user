package ru.majordomo.hms.rc.user.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteCreateEvent;
import ru.majordomo.hms.rc.user.event.webSite.WebSiteImportEvent;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.CharSet;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

@Component
public class WebSiteDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final WebSiteRepository webSiteRepository;
    private final DomainRepository domainRepository;
    private final StaffResourceControllerClient staffResourceControllerClient;
    private final ApplicationEventPublisher publisher;

    public static final Map<String, CharSet> STRING_CHAR_SET_HASH_MAP = new HashMap<>();

    private String[] defaultWebsiteIndexFileList;

    static {
        STRING_CHAR_SET_HASH_MAP.put("CP-1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("cp1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("windows-1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("windows-cp1251", CharSet.CP1251);
        STRING_CHAR_SET_HASH_MAP.put("koi8-r", CharSet.KOI8R);
        STRING_CHAR_SET_HASH_MAP.put("utf-8", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("utf8", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("iso-8859-1", CharSet.UTF8);
        STRING_CHAR_SET_HASH_MAP.put("", CharSet.UTF8);
    }

    @Value("${default.website.indexFileList}")
    public void setDefaultWebsiteIndexFileList(String[] defaultWebsiteIndexFileList) {
        this.defaultWebsiteIndexFileList = defaultWebsiteIndexFileList;
    }

    @Autowired
    public WebSiteDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            WebSiteRepository webSiteRepository,
            DomainRepository domainRepository,
            StaffResourceControllerClient staffResourceControllerClient,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.webSiteRepository = webSiteRepository;
        this.domainRepository = domainRepository;
        this.staffResourceControllerClient = staffResourceControllerClient;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id, v.ServerName " +
                "FROM vhosts v " +
                "LEFT JOIN domain d ON v.ServerName = d.Domain_name " +
                "JOIN account a ON v.uid = a.uid " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new WebSiteImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.uid, a.server_id, a.homedir, " +
                "v.active, v.mod_php, v.mod_ssl, v.charset_disable, v.server, v.vhname, v.ServerName, " +
                "v.DocumentRoot, v.VirtualDocumentRoot, v.ScriptAlias, v.VirtualScriptAlias, v.CustomLog, " +
                "v.ErrorLog, v.DirectoryIndex, v.CharsetSourceEnc, v.Options, v.ServerAlias, v.apache_conf, " +
                "v.log_errors, v.prodfarm_id, v.anti_ddos, v.auto_subdomain, v.nginx_static, " +
                "v.unsecure_admin_panel_access, v.perl_lib_path, " +
                "d.Domain_name, " +
                "s.id as web_id, " +
                "nc.flag " +
                "FROM vhosts v " +
                "LEFT JOIN domain d ON v.ServerName = d.Domain_name " +
                "LEFT JOIN servers s ON CONCAT(s.name, '.majordomo.ru') = v.server " +
                "LEFT JOIN nginx_conf nc ON nc.redir_to = v.vhname AND nc.server = v.server " +
                "JOIN account a ON v.uid = a.uid " +
                "WHERE a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private WebSite rowMap(ResultSet rs, int rowNum) throws SQLException {
        //Тестовые домены шлём в сад
        if (rs.getString("ServerName").endsWith(".onparking.ru")) {
            return null;
        }

        String accountId = rs.getString("id");
        String serverName = rs.getString("ServerName");
        String unicodeServerName = java.net.IDN.toUnicode(serverName);
        logger.debug("Found Mailbox for id: " + accountId + " name: " + unicodeServerName);

        WebSite webSite = new WebSite();
        webSite.setAccountId(accountId);
        webSite.setSwitchedOn(true);
        webSite.setName(unicodeServerName);
        webSite.setUnixAccountId("unixAccount_" + rs.getString("id"));
        webSite.setCharSet(STRING_CHAR_SET_HASH_MAP.get(rs.getString("CharsetSourceEnc")));
        webSite.setDocumentRoot(rs.getString("homedir") + rs.getString("DocumentRoot"));
        webSite.setAutoSubDomain(rs.getString("auto_subdomain").equals("Y"));
        webSite.setDdosProtection(rs.getString("anti_ddos").equals("Y"));
        webSite.setScriptAlias(rs.getString("ScriptAlias"));
        webSite.setAccessLogEnabled(true);
        webSite.setErrorLogEnabled(true);
        webSite.setAccessByOldHttpVersion(rs.getString("unsecure_admin_panel_access").equals("Y"));

        //SSI
        Pattern p = Pattern.compile("AddHandler\\s+server-parsed\\s+(.*)", CASE_INSENSITIVE);
        Matcher m = p.matcher(rs.getString("apache_conf"));
        webSite.setSsiEnabled(m.matches());
        if (m.matches()) {
            String[] ssiFileExtensions = m.group(1).split(" ");
            webSite.setSsiFileExtensions(Arrays.asList(ssiFileExtensions));
        }

        //CGI
        p = Pattern.compile("-ExecCGI", CASE_INSENSITIVE);
        m = p.matcher(rs.getString("Options"));
        webSite.setCgiEnabled(m.matches());

        //CGI FileExtensions
        p = Pattern.compile("AddHandler\\s+cgi-script\\s+(.*)", CASE_INSENSITIVE);
        m = p.matcher(rs.getString("apache_conf"));
        if (m.matches()) {
            String[] cgiFileExtensions = m.group(1).split(" ");
            webSite.setCgiFileExtensions(Arrays.asList(cgiFileExtensions));
        }

        //StaticFileExtensions
        webSite.setStaticFileExtensions(Arrays.asList(rs.getString("nginx_static").split(" ")));

        //IndexFileList
        String directoryIndex = rs.getString("DirectoryIndex");
        List<String> indexFileList;

        if (directoryIndex != null && !directoryIndex.equals("")) {
            indexFileList = Arrays.asList(directoryIndex.split(" "));
        } else {
            indexFileList = Arrays.asList(defaultWebsiteIndexFileList);
        }
        webSite.setIndexFileList(indexFileList);

        String serverId = "web_server_" + rs.getString("web_id");
        String serviceType = "WEBSITE_APACHE2_" + rs.getString("flag").toUpperCase().replaceAll("-", "_");
        serviceType = serviceType.matches("^.*[34560]$") ? serviceType + "_DEFAULT" : serviceType;

        logger.debug("Searching for service: " + serverId + " , " + serviceType);

        List<Service> services = staffResourceControllerClient.getServicesByServerIdAndServiceType(
                serverId,
                serviceType
        );

        if(!services.isEmpty()) {
            webSite.setServiceId(services.get(0).getId());
        } else {
            logger.error("getServicesByServerIdAndServiceType isEmpty for serverId: " + serverId + " and serviceType: " + serviceType);
        }

        Domain domain = domainRepository.findByNameAndAccountId(
                unicodeServerName,
                accountId
        );

        if (domain != null) {
            webSite.addDomain(domain);
        }

        //Алиасы
        String query = "SELECT e.Domain_name, e.value as name, e.acc_id as id " +
                "FROM extend e " +
                "WHERE e.usluga = 2  AND e.acc_id = :accountId AND e.Domain_name = :domainName";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId)
                .addValue("domainName", "serverName");

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                ((rs1, rowNum1) -> {
                    Domain aliasDomain = domainRepository.findByNameAndAccountId(
                            java.net.IDN.toUnicode(rs1.getString("value")),
                            accountId
                    );

                    if (aliasDomain != null) {
                        webSite.addDomain(aliasDomain);
                    }

                    return null;
                })
        );

        publisher.publishEvent(new WebSiteCreateEvent(webSite));

        return null;
    }

    public boolean importToMongo() {
        webSiteRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<WebSite> webSites = webSiteRepository.findByAccountId(accountId);

        if (webSites != null && !webSites.isEmpty()) {
            webSiteRepository.delete(webSites);
        }

        pull(accountId);
        return true;
    }
}
