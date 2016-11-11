package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.CharSet;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.rc.user.common.CharSet.UTF8;

@Component
public class GovernorOfWebSite extends LordOfResources {

    private WebSiteRepository repository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;
    private String defaultServiceName;
    private String defaultWebsiteDocumetRootPattern;
    private String defaultWebsiteCharset;
    private Boolean defaultWebsiteSsiEnabled;
    private String[] defaultWebsiteSsiFileExtensions;
    private Boolean defaultWebsiteCgiEnabled;
    private String[] defaultWebsiteCgiFileExtensions;
    private String defaultWebsiteScriptAliace;
    private Boolean defaultWebsiteDdosProtection;
    private Boolean defaultWebsiteAutoSubDomain;
    private Boolean defaultWebsiteAccessByOldHttpVersion;
    private String[] defaultWebsiteStaticFileExtensions;
    private String[] defaultWebsiteIndexFileList;
    private String defaultWebsiteCustomUserConf;
    private Boolean defaultAccessLogEnabled;
    private Boolean defaultErrorLogEnabled;

    @Value("${default.website.service.name}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Value("${default.website.documet.root.pattern}")
    public void setDefaultWebsiteDocumetRootPattern(String defaultWebsiteDocumetRootPattern) {
        this.defaultWebsiteDocumetRootPattern = defaultWebsiteDocumetRootPattern;
    }

    @Value("${default.website.charset}")
    public void setDefaultWebsiteCharset(String defaultWebsiteCharset) {
        this.defaultWebsiteCharset = defaultWebsiteCharset;
    }

    @Value("${default.website.ssi.enabled}")
    public void setDefaultWebsiteSsiEnabled(Boolean defaultWebsiteSsiEnabled) {
        this.defaultWebsiteSsiEnabled = defaultWebsiteSsiEnabled;
    }

    @Value("${default.website.ssi.file.extensions}")
    public void setDefaultWebsiteSsiFileExtensions(String[] defaultWebsiteSsiFileExtensions) {
        this.defaultWebsiteSsiFileExtensions = defaultWebsiteSsiFileExtensions;
    }

    @Value("${default.website.cgi.enabled}")
    public void setDefaultWebsiteCgiEnabled(Boolean defaultWebsiteCgiEnabled) {
        this.defaultWebsiteCgiEnabled = defaultWebsiteCgiEnabled;
    }

    @Value("${default.website.cgi.file.extensions}")
    public void setDefaultWebsiteCgiFileExtensions(String[] defaultWebsiteCgiFileExtensions) {
        this.defaultWebsiteCgiFileExtensions = defaultWebsiteCgiFileExtensions;
    }

    @Value("${default.website.script.aliace}")
    public void setDefaultWebsiteScriptAliace(String defaultWebsiteScriptAliace) {
        this.defaultWebsiteScriptAliace = defaultWebsiteScriptAliace;
    }

    @Value("${default.website.ddos.protection}")
    public void setDefaultWebsiteDdosProtection(Boolean defaultWebsiteDdosProtection) {
        this.defaultWebsiteDdosProtection = defaultWebsiteDdosProtection;
    }

    @Value("${default.website.auto.sub.domain}")
    public void setDefaultWebsiteAutoSubDomain(Boolean defaultWebsiteAutoSubDomain) {
        this.defaultWebsiteAutoSubDomain = defaultWebsiteAutoSubDomain;
    }

    @Value("${default.website.access.by.old.http.version}")
    public void setDefaultWebsiteAccessByOldHttpVersion(Boolean defaultWebsiteAccessByOldHttpVersion) {
        this.defaultWebsiteAccessByOldHttpVersion = defaultWebsiteAccessByOldHttpVersion;
    }

    @Value("${default.website.static.file.extensions}")
    public void setDefaultWebsiteStaticFileExtensions(String[] defaultWebsiteStaticFileExtensions) {
        this.defaultWebsiteStaticFileExtensions = defaultWebsiteStaticFileExtensions;
    }

    @Value("${default.website.index.file.list}")
    public void setDefaultWebsiteIndexFileList(String[] defaultWebsiteIndexFileList) {
        this.defaultWebsiteIndexFileList = defaultWebsiteIndexFileList;
    }

    @Value("${default.website.custom.user.conf}")
    public void setDefaultWebsiteCustomUserConf(String defaultWebsiteCustomUserConf) {
        this.defaultWebsiteCustomUserConf = defaultWebsiteCustomUserConf;
    }

    @Value("${default.website.access.log.enabled}")
    public void setDefaultAccessLogEnabled(Boolean defaultAccessLogEnabled) {
        this.defaultAccessLogEnabled = defaultAccessLogEnabled;
    }

    @Value("${default.website.error.log.enabled}")
    public void setDefaultErrorLogEnabled(Boolean defaultErrorLogEnabled) {
        this.defaultErrorLogEnabled = defaultErrorLogEnabled;
    }


    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setGovernorOfUnixAccount(GovernorOfUnixAccount governorOfUnixAccount) {
        this.governorOfUnixAccount = governorOfUnixAccount;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setRepository(WebSiteRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        WebSite webSite;
        try {
            webSite = (WebSite) buildResourceFromServiceMessage(serviceMessage);
            validate(webSite);
            store(webSite);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return webSite;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        WebSite webSite = new WebSite();

        LordOfResources.setResourceParams(webSite, serviceMessage, cleaner);

        List<String> domainIds = new ArrayList<>();
        if (serviceMessage.getParam("domainIds") != null) {
            domainIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("domainIds"));
        }
        for (String domainId : domainIds) {
            Domain domain = (Domain) governorOfDomain.build(domainId);
            webSite.addDomain(domain);
        }

        String applicationServiceId = cleaner.cleanString((String) serviceMessage.getParam("applicationService"));
        String documentRoot = cleaner.cleanString((String) serviceMessage.getParam("documentRoot"));

        String unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));
        UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(unixAccountId);

        CharSet charSet = null;
        String charsetAsString;
        if (serviceMessage.getParam("charSet") != null) {
            charsetAsString = cleaner.cleanString((String) serviceMessage.getParam("charSet"));
            charSet = Enum.valueOf(CharSet.class, charsetAsString);
        }

        Boolean ssiEnabled = (Boolean) serviceMessage.getParam("ssiEnabled");
        List<String> ssiFileExtensions = new ArrayList<>();
        if (serviceMessage.getParam("ssiFileExtensions") != null) {
            ssiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("ssiFileExtensions"));
        }
        Boolean cgiEnabled = (Boolean) serviceMessage.getParam("cgiEnabled");
        List<String> cgiFileExtensions = new ArrayList<>();
        if (serviceMessage.getParam("cgiFileExtensions") != null) {
            cgiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("cgiFileExtensions"));
        }
        String scriptAlias = cleaner.cleanString((String) serviceMessage.getParam("scriptAlias"));
        Boolean autoSubDomain = (Boolean) serviceMessage.getParam("autoSubDomain");
        Boolean accessByOldHttpVersion = (Boolean) serviceMessage.getParam("accessByOldHttpVersion");
        List<String> staticFileExtensions = new ArrayList<>();
        if (serviceMessage.getParam("staticFileExtensions") != null) {
            staticFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("staticFileExtensions"));
        }
        List<String> indexFileList = new ArrayList<>();
        if (serviceMessage.getParam("indexFileList") != null) {
            indexFileList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("indexFileList"));
        }
        Boolean accessLogEnabled = (Boolean) serviceMessage.getParam("accessLogEnabled");
        Boolean errorLogEnabled = (Boolean) serviceMessage.getParam("errorLogEnabled");

        webSite.setServiceId(applicationServiceId);
        webSite.setDocumentRoot(documentRoot);
        if (unixAccount != null) {
            webSite.setUnixAccount(unixAccount);
        }
        webSite.setCharSet(charSet);
        webSite.setSsiEnabled(ssiEnabled);
        webSite.setSsiFileExtensions(ssiFileExtensions);
        webSite.setCgiEnabled(cgiEnabled);
        webSite.setCgiFileExtensions(cgiFileExtensions);
        webSite.setScriptAlias(scriptAlias);
        webSite.setAutoSubDomain(autoSubDomain);
        webSite.setAccessByOldHttpVersion(accessByOldHttpVersion);
        webSite.setStaticFileExtensions(staticFileExtensions);
        webSite.setIndexFileList(indexFileList);
        webSite.setAccessLogEnabled(accessLogEnabled);
        webSite.setErrorLogEnabled(errorLogEnabled);

        return webSite;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        WebSite webSite = (WebSite) resource;

        if (webSite.getAccountId() == null || webSite.getAccountId().equals("")) {
            throw new ParameterValidateException("Аккаунт ID не может быть пустым");
        }

        if (webSite.getDomains().isEmpty()) {
            throw new ParameterValidateException("Должен присутствовать хотя бы один домен");
        }

        if (webSite.getName() == null || webSite.getName().equals("")) {
            webSite.setName(webSite.getDomains().get(0).getName());
        }

        if (webSite.getSwitchedOn() == null) {
            webSite.setSwitchedOn(true);
        }

        if (webSite.getDocumentRoot() == null || webSite.getDocumentRoot().equals("")) {
            webSite.setDocumentRoot(webSite.getDomains().get(0).getName() + defaultWebsiteDocumetRootPattern);
        }


        if (webSite.getUnixAccount() == null) {
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("accountId", webSite.getAccountId());
            List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
            if (unixAccounts == null || unixAccounts.isEmpty()) {
                throw new ParameterValidateException("Для создания WebSite необходим UnixAccount");
            }
            webSite.setUnixAccount(unixAccounts.get(0));
        }

        List<Service> websiteServices = staffRcClient.getWebsiteServicesByServerIdAndServiceType(webSite.getUnixAccount().getServerId());
        if (webSite.getServiceId() == null || (webSite.getServiceId().equals(""))) {
            for (Service service : websiteServices) {
                if (service.getServiceType().getName().equals(this.defaultServiceName)) {
                    webSite.setServiceId(service.getId());
                    break;
                }
            }
            if (webSite.getServiceId() == null || (webSite.getServiceId().equals(""))) {
                throw new ParameterValidateException("Не найдено serviceType: " + this.defaultServiceName + " для сервера: " + webSite.getUnixAccount().getServerId());
            }
        } else {
            Boolean isServiceForServerExist = false;
            for (Service service : websiteServices) {
                if (service.getId().equals(webSite.getServiceId())) {
                    isServiceForServerExist = true;
                    break;
                }
            }
            if (!isServiceForServerExist) {
                throw new ParameterValidateException("Указанный ServiceId: " + webSite.getServiceId() + " не найден для сервера: " + webSite.getUnixAccount().getServerId());
            }
        }


        if (webSite.getCharSet() == null) {
            CharSet charSet = CharSet.valueOf(defaultWebsiteCharset);
            webSite.setCharSet(charSet);
        }

        if (webSite.getSsiEnabled() == null) {
            webSite.setSsiEnabled(defaultWebsiteSsiEnabled);
        }

        if (webSite.getSsiFileExtensions() == null || webSite.getSsiFileExtensions().isEmpty()) {
            webSite.setSsiFileExtensions(Arrays.asList(defaultWebsiteSsiFileExtensions));
        }

        if (webSite.getCgiEnabled() == null) {
            webSite.setCgiEnabled(defaultWebsiteCgiEnabled);
        }

        if (webSite.getCgiFileExtensions() == null || webSite.getCgiFileExtensions().isEmpty()) {
            webSite.setCgiFileExtensions(Arrays.asList(defaultWebsiteCgiFileExtensions));
        }

        if (webSite.getScriptAlias() == null || webSite.getScriptAlias().equals("")) {
            webSite.setScriptAlias(defaultWebsiteScriptAliace);
        }

        if (webSite.getDdosProtection() == null) {
            webSite.setDdosProtection(defaultWebsiteDdosProtection);
        }

        if (webSite.getAutoSubDomain() == null) {
            webSite.setAutoSubDomain(defaultWebsiteAutoSubDomain);
        }

        if (webSite.getAccessByOldHttpVersion() == null) {
            webSite.setAccessByOldHttpVersion(defaultWebsiteAccessByOldHttpVersion);
        }

        if (webSite.getStaticFileExtensions() == null || webSite.getStaticFileExtensions().isEmpty()) {
            webSite.setStaticFileExtensions(Arrays.asList(defaultWebsiteStaticFileExtensions));
        }

        if (webSite.getCustomUserConf() == null) {
            webSite.setCustomUserConf(defaultWebsiteCustomUserConf);
        }

        if (webSite.getIndexFileList() == null || webSite.getIndexFileList().isEmpty()) {
            webSite.setIndexFileList(Arrays.asList(defaultWebsiteIndexFileList));
        }

        if (webSite.getAccessLogEnabled() == null) {
            webSite.setAccessLogEnabled(defaultAccessLogEnabled);
        }

        if (webSite.getErrorLogEnabled() == null) {
            webSite.setErrorLogEnabled(defaultErrorLogEnabled);
        }

    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        WebSite webSite = (WebSite) resource;
        for (String domainId : webSite.getDomainIds()) {
            Domain domain = (Domain) governorOfDomain.build(domainId);
            webSite.addDomain(domain);
        }
        String unixAccountId = webSite.getUnixAccountId();
        UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(unixAccountId);
        webSite.setUnixAccount(unixAccount);

        return webSite;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        WebSite webSite = repository.findOne(resourceId);

        return construct(webSite);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        WebSite website = new WebSite();

        boolean byAccountId = false;
        boolean byId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("websiteId")) {
                byId = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId && byId) {
            website = (WebSite) construct(repository.findByIdAndAccountId(keyValue.get("websiteId"), keyValue.get("accountId")));
        }

        return website;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<WebSite> buildedWebSites = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            for (WebSite webSite : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedWebSites.add((WebSite) construct(webSite));
            }
        }

        return buildedWebSites;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<WebSite> buildedWebSites = new ArrayList<>();

        for (WebSite webSite : repository.findAll()) {
            buildedWebSites.add((WebSite) construct(webSite));
        }

        return buildedWebSites;
    }

    @Override
    public void store(Resource resource) {
        WebSite webSite = (WebSite) resource;
        repository.save(webSite);
    }

}
