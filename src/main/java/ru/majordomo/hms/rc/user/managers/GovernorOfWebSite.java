package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.resources.CharSet;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;
import ru.majordomo.hms.rc.user.validation.group.WebSiteChecks;

@Component
public class GovernorOfWebSite extends LordOfResources<WebSite> {

    private WebSiteRepository repository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private GovernorOfResourceArchive governorOfResourceArchive;
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
    private Boolean defaultFollowSymLinks;
    private Boolean defaultMultiViews;
    private Integer defaultMbstringFuncOverload;
    private Boolean defaultAllowUrlFopen;
    private Validator validator;

    @Value("${default.website.serviceName}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Value("${default.website.documentRootPattern}")
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

    @Value("${default.website.ssi.fileExtensions}")
    public void setDefaultWebsiteSsiFileExtensions(String[] defaultWebsiteSsiFileExtensions) {
        this.defaultWebsiteSsiFileExtensions = defaultWebsiteSsiFileExtensions;
    }

    @Value("${default.website.cgi.enabled}")
    public void setDefaultWebsiteCgiEnabled(Boolean defaultWebsiteCgiEnabled) {
        this.defaultWebsiteCgiEnabled = defaultWebsiteCgiEnabled;
    }

    @Value("${default.website.cgi.fileExtensions}")
    public void setDefaultWebsiteCgiFileExtensions(String[] defaultWebsiteCgiFileExtensions) {
        this.defaultWebsiteCgiFileExtensions = defaultWebsiteCgiFileExtensions;
    }

    @Value("${default.website.scriptAlias}")
    public void setDefaultWebsiteScriptAliace(String defaultWebsiteScriptAliace) {
        this.defaultWebsiteScriptAliace = defaultWebsiteScriptAliace;
    }

    @Value("${default.website.ddosProtection}")
    public void setDefaultWebsiteDdosProtection(Boolean defaultWebsiteDdosProtection) {
        this.defaultWebsiteDdosProtection = defaultWebsiteDdosProtection;
    }

    @Value("${default.website.autoSubDomain}")
    public void setDefaultWebsiteAutoSubDomain(Boolean defaultWebsiteAutoSubDomain) {
        this.defaultWebsiteAutoSubDomain = defaultWebsiteAutoSubDomain;
    }

    @Value("${default.website.accessByOldHttpVersion}")
    public void setDefaultWebsiteAccessByOldHttpVersion(Boolean defaultWebsiteAccessByOldHttpVersion) {
        this.defaultWebsiteAccessByOldHttpVersion = defaultWebsiteAccessByOldHttpVersion;
    }

    @Value("${default.website.static.fileExtensions}")
    public void setDefaultWebsiteStaticFileExtensions(String[] defaultWebsiteStaticFileExtensions) {
        this.defaultWebsiteStaticFileExtensions = defaultWebsiteStaticFileExtensions;
    }

    @Value("${default.website.indexFileList}")
    public void setDefaultWebsiteIndexFileList(String[] defaultWebsiteIndexFileList) {
        this.defaultWebsiteIndexFileList = defaultWebsiteIndexFileList;
    }

    @Value("${default.website.customUserConf}")
    public void setDefaultWebsiteCustomUserConf(String defaultWebsiteCustomUserConf) {
        this.defaultWebsiteCustomUserConf = defaultWebsiteCustomUserConf;
    }

    @Value("${default.website.accessLogEnabled}")
    public void setDefaultAccessLogEnabled(Boolean defaultAccessLogEnabled) {
        this.defaultAccessLogEnabled = defaultAccessLogEnabled;
    }

    @Value("${default.website.errorLogEnabled}")
    public void setDefaultErrorLogEnabled(Boolean defaultErrorLogEnabled) {
        this.defaultErrorLogEnabled = defaultErrorLogEnabled;
    }

    @Value("${default.website.mbstringFuncOverload}")
    public void setDefaultMbstringFuncOverload(Integer defaultMbstringFuncOverload) {
        this.defaultMbstringFuncOverload = defaultMbstringFuncOverload;
    }

    @Value("${default.website.allowUrlFopen}")
    public void setDefaultAllowUrlFopen(Boolean defaultAllowUrlFopen) {
        this.defaultAllowUrlFopen = defaultAllowUrlFopen;
    }

    @Value("${default.website.followSymLinks}")
    public void setDefaultFollowSymLinks(Boolean defaultFollowSymLinks) {
        this.defaultFollowSymLinks = defaultFollowSymLinks;
    }

    @Value("${default.website.multiViews}")
    public void setDefaultMultiViews(Boolean defaultMultiViews) {
        this.defaultMultiViews = defaultMultiViews;
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
    public void setGovernorOfResourceArchive(GovernorOfResourceArchive governorOfResourceArchive) {
        this.governorOfResourceArchive = governorOfResourceArchive;
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

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public WebSite update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        WebSite website = build(keyValue);

        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "name":
                        website.setName(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "domainIds":
                        website.setDomainIds(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        for (String domainId : website.getDomainIds()) {
                            Domain domain = governorOfDomain.build(domainId);
                            website.addDomain(domain);
                        }
                        break;
                    case "applicationServiceId":
                        website.setServiceId(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "documentRoot":
                        String documentRoot = cleaner.cleanString((String) entry.getValue());
                        if (documentRoot.startsWith("/")) {
                            documentRoot = documentRoot.substring(1);
                        }
                        website.setDocumentRoot(documentRoot);
                        break;
                    case "charSet":
                        String charsetAsString = cleaner.cleanString((String) serviceMessage.getParam("charSet"));
                        website.setCharSet(Enum.valueOf(CharSet.class, charsetAsString));
                        break;
                    case "ssiEnabled":
                        website.setSsiEnabled((Boolean) entry.getValue());
                        break;
                    case "ssiFileExtensions":
                        List<String> ssiFileExtensions = new ArrayList<>();
                        if (website.getSsiEnabled()) {
                            ssiFileExtensions = cleaner.cleanListWithStrings((List<String>) entry.getValue());
                        }
                        website.setSsiFileExtensions(ssiFileExtensions);
                        break;
                    case "cgiEnabled":
                        website.setCgiEnabled((Boolean) entry.getValue());
                        break;
                    case "cgiFileExtensions":
                        List<String> cgiFileExtensions = new ArrayList<>();
                        if (website.getSsiEnabled()) {
                            cgiFileExtensions = cleaner.cleanListWithStrings((List<String>) entry.getValue());
                        }
                        website.setCgiFileExtensions(cgiFileExtensions);
                        break;
                    case "scriptAlias":
                        website.setScriptAlias(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "autoSubDomain":
                        website.setAutoSubDomain((Boolean) entry.getValue());
                        break;
                    case "accessByOldHttpVersion":
                        website.setAccessByOldHttpVersion((Boolean) entry.getValue());
                        break;
                    case "staticFileExtensions":
                        website.setStaticFileExtensions(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "indexFileList":
                        website.setIndexFileList(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "accessLogEnabled":
                        website.setAccessLogEnabled((Boolean) entry.getValue());
                        break;
                    case "errorLogEnabled":
                        website.setErrorLogEnabled((Boolean) entry.getValue());
                        break;
                    case "allowUrlFopen":
                        website.setAllowUrlFopen((Boolean) entry.getValue());
                        break;
                    case "mbstringFuncOverload":
                        website.setMbstringFuncOverload((Integer) entry.getValue());
                        break;
                    case "followSymLinks":
                        website.setFollowSymLinks((Boolean) entry.getValue());
                        break;
                    case "multiViews":
                        website.setMultiViews((Boolean) entry.getValue());
                        break;
                    case "switchedOn":
                        website.setSwitchedOn((Boolean) entry.getValue());
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        preValidate(website);
        validate(website);
        store(website);

        return website;
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfResourceArchive.dropByResourceId(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected WebSite buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        WebSite webSite = new WebSite();

        setResourceParams(webSite, serviceMessage, cleaner);

        try {
            List<String> domainIds = new ArrayList<>();
            if (serviceMessage.getParam("domainIds") != null) {
                domainIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("domainIds"));
            }
            for (String domainId : domainIds) {
                Domain domain = governorOfDomain.build(domainId);
                webSite.addDomain(domain);
            }

            String applicationServiceId = cleaner.cleanString((String) serviceMessage.getParam("applicationServiceId"));
            String documentRoot = cleaner.cleanString((String) serviceMessage.getParam("documentRoot"));
            if (documentRoot.startsWith("/")) {
                documentRoot = documentRoot.substring(1);
            }

            String unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));

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
            Boolean allowUrlFopen = (Boolean) serviceMessage.getParam("allowUrlFopen");
            Integer mbstringFuncOverload = (Integer) serviceMessage.getParam("mbstringFuncOverload");


            webSite.setServiceId(applicationServiceId);
            webSite.setDocumentRoot(documentRoot);
            webSite.setUnixAccountId(unixAccountId);
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
            webSite.setAllowUrlFopen(allowUrlFopen);
            webSite.setMbstringFuncOverload(mbstringFuncOverload);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        return webSite;
    }

    @Override
    public void preValidate(WebSite webSite) {
        if ((webSite.getName() == null || webSite.getName().equals("")) && !webSite.getDomains().isEmpty()) {
            webSite.setName(webSite.getDomains().get(0).getName());
        }

        if (webSite.getSwitchedOn() == null) {
            webSite.setSwitchedOn(true);
        }

        if ((webSite.getDocumentRoot() == null || webSite.getDocumentRoot().equals("")) && !webSite.getDomains().isEmpty()) {
            webSite.setDocumentRoot(webSite.getDomains().get(0).getName() + defaultWebsiteDocumetRootPattern);
        }

        if (webSite.getUnixAccountId() == null || webSite.getUnixAccountId().equals("")) {
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("accountId", webSite.getAccountId());
            List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
            if (unixAccounts == null || unixAccounts.isEmpty()) {
                throw new ParameterValidateException("Для создания WebSite необходим UnixAccount");
            }
            webSite.setUnixAccount(unixAccounts.get(0));
        } else {
            webSite.setUnixAccount(governorOfUnixAccount.build(webSite.getUnixAccountId()));
        }

        if (webSite.getServiceId() == null || (webSite.getServiceId().equals(""))) {
            List<Service> websiteServices = staffRcClient.getWebsiteServicesByServerId(webSite.getUnixAccount().getServerId());
            for (Service service : websiteServices) {
                if (service.getServiceTemplate().getServiceType().getName().equals(this.defaultServiceName)) {
                    webSite.setServiceId(service.getId());
                    break;
                }
            }
            if (webSite.getServiceId() == null || (webSite.getServiceId().equals(""))) {
                logger.error("Не найдено serviceType: " + this.defaultServiceName
                        + " для сервера: " + webSite.getUnixAccount().getServerId());
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

        if (webSite.getStaticFileExtensions() == null) {
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

        if (webSite.getMbstringFuncOverload() == null || webSite.getMbstringFuncOverload() < 0 || webSite.getMbstringFuncOverload() > 7) {
            webSite.setMbstringFuncOverload(defaultMbstringFuncOverload);
        }

        if (webSite.getAllowUrlFopen() == null) {
            webSite.setAllowUrlFopen(defaultAllowUrlFopen);
        }

        if (webSite.getFollowSymLinks() == null) {
            webSite.setFollowSymLinks(defaultFollowSymLinks);
        }

        if (webSite.getMultiViews() == null) {
            webSite.setMultiViews(defaultMultiViews);
        }
    }

    @Override
    public void validate(WebSite webSite) throws ParameterValidateException {
        Set<ConstraintViolation<WebSite>> constraintViolations = validator.validate(webSite, WebSiteChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.error(constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected WebSite construct(WebSite webSite) throws ParameterValidateException {
        for (String domainId : webSite.getDomainIds()) {
            Domain domain = governorOfDomain.build(domainId);
            webSite.addDomain(domain);
        }
        String unixAccountId = webSite.getUnixAccountId();
        UnixAccount unixAccount = governorOfUnixAccount.build(unixAccountId);
        webSite.setUnixAccount(unixAccount);

        return webSite;
    }

    @Override
    public WebSite build(String resourceId) throws ResourceNotFoundException {
        WebSite webSite = repository.findOne(resourceId);
        if (webSite == null) {
            throw new ResourceNotFoundException("Сайт с ID " + resourceId + "не найден");
        }
        return construct(webSite);
    }

    @Override
    public WebSite build(Map<String, String> keyValue) throws ResourceNotFoundException {
        WebSite website = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            website = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (keyValue.get("domainId") != null) {
            if (keyValue.get("accountId") != null) {
                website = repository.findByDomainIdsContainsAndAccountId(keyValue.get("domainId"), keyValue.get("accountId"));
            } else {
                website = repository.findByDomainIdsContains(keyValue.get("domainId"));
            }
        }

        if (website == null) {
            throw new ResourceNotFoundException("Не найден WebSite по параметрам: " + keyValue.toString());
        }

        return construct(website);
    }

    @Override
    public Collection<WebSite> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<WebSite> buildedWebSites = new ArrayList<>();

        if (keyValue.get("unixAccountId") != null) {
            for (WebSite webSite : repository.findByUnixAccountId(keyValue.get("unixAccountId"))) {
                buildedWebSites.add(construct(webSite));
            }
        } else if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            for (WebSite webSite : repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId"))) {
                buildedWebSites.add(construct(webSite));
            }
        } else if (keyValue.get("accountId") != null) {
            for (WebSite webSite : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedWebSites.add(construct(webSite));
            }
        } else if (keyValue.get("serviceId") != null) {
            for (WebSite webSite : repository.findByServiceId(keyValue.get("serviceId"))) {
                buildedWebSites.add(construct(webSite));
            }
        }

        return buildedWebSites;
    }

    @Override
    public Collection<WebSite> buildAll() {
        List<WebSite> buildedWebSites = new ArrayList<>();

        for (WebSite webSite : repository.findAll()) {
            buildedWebSites.add(construct(webSite));
        }

        return buildedWebSites;
    }

    @Override
    public void store(WebSite webSite) {
        repository.save(webSite);
    }

    public Count countByAccountId(String accountId) {
        Count count = new Count();
        count.setCount(repository.countByAccountId(accountId));
        return count;
    }

}
