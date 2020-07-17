package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.validation.group.WebSiteChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.WebSiteImportChecks;

@Component
public class GovernorOfWebSite extends LordOfResources<WebSite> {

    private WebSiteRepository repository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private GovernorOfResourceArchive governorOfResourceArchive;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;
    private Validator validator;
    private DefaultWebSiteSettings defaultWebSiteSettings;

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

    @Autowired
    public void setDefaultWebSiteSettings(DefaultWebSiteSettings defaultWebSiteSettings) {
        this.defaultWebSiteSettings = defaultWebSiteSettings;
    }

    @Override
    public WebSite update(ServiceMessage serviceMessage) throws ParameterValidationException {
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
                    case "serviceId":
                        website.setServiceId(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "documentRoot":
                        String documentRoot = cleaner.cleanString((String) entry.getValue());
                        website.setDocumentRoot(documentRoot);
                        break;
                    case "charSet":
                        String charsetAsString = cleaner.cleanString((String) serviceMessage.getParam("charSet"));
                        website.setCharSet(Enum.valueOf(CharSet.class, charsetAsString));
                        break;
                    case "ssiEnabled":
                        website.setSsiEnabled(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "ssiFileExtensions":
                        List<String> ssiFileExtensions = new ArrayList<>();
                        if (website.getSsiEnabled()) {
                            ssiFileExtensions = cleaner.cleanListWithStrings((List<String>) entry.getValue());
                        }
                        website.setSsiFileExtensions(ssiFileExtensions);
                        break;
                    case "cgiEnabled":
                        website.setCgiEnabled(cleaner.cleanBoolean(entry.getValue()));
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
                        website.setAutoSubDomain(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "accessByOldHttpVersion":
                        website.setAccessByOldHttpVersion(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "staticFileExtensions":
                        website.setStaticFileExtensions(cleaner.cleanListWithStrings((List<String>) entry.getValue()));

                        break;
                    case "indexFileList":
                        website.setIndexFileList(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "accessLogEnabled":
                        website.setAccessLogEnabled(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "errorLogEnabled":
                        website.setErrorLogEnabled(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "allowUrlFopen":
                        website.setAllowUrlFopen(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "mbstringFuncOverload":
                        website.setMbstringFuncOverload(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "displayErrors":
                        website.setDisplayErrors(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "sessionUseTransSid":
                        website.setSessionUseTransSid(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "maxInputVars":
                        website.setMaxInputVars(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "opcacheMaxAcceleratedFiles":
                        website.setOpcacheMaxAcceleratedFiles(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "realpathCacheSize":
                        website.setRealpathCacheSize(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "requestOrder":
                        website.setRequestOrder((String) entry.getValue());
                        break;
                    case "allowUrlInclude":
                        website.setAllowUrlInclude(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "opcacheRevalidateFreq":
                        website.setOpcacheRevalidateFreq(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "memoryLimit":
                        website.setMemoryLimit(cleaner.cleanInteger(entry.getValue()));
                        break;
                    case "mbstringInternalEncoding":
                        website.setMbstringInternalEncoding((String) entry.getValue());
                        break;
                    case "followSymLinks":
                        website.setFollowSymLinks(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "multiViews":
                        website.setMultiViews(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "ddosProtection":
                        website.setDdosProtection(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "switchedOn":
                        website.setSwitchedOn(cleaner.cleanBoolean(entry.getValue()));
                        break;
                    case "appInstallCommands":
                        website.setAppInstallCommands(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "appLoadUrl":
                        website.setAppLoadUrl(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "appLoadParams":
                        website.setAppLoadParams(cleaner.cleanMapWithStrings(entry.getValue()));
                        break;
                    case "pythonModule":
                        website.setPythonModule(cleaner.cleanString(entry.getValue()));
                        break;
                    case "staticFileDirs":
                        website.setStaticFileDirs(cleaner.cleanListWithStrings(entry.getValue()));
                        break;
                    case "appUpdateCommands":
                        website.setAppUpdateCommands(cleaner.cleanString(entry.getValue()));
                        break;
                    case "staticRoot":
                        website.setStaticRoot(cleaner.cleanString(entry.getValue()));
                    default:
                        break;
                } // switch
            } // for
            if (serviceMessage.getParam("expiresForTypes") != null) {
                //все расширения из expiresForTypes должны быть в staticFileExtensions
                Map<String, String> expiresRaw = cleaner.cleanMapWithStrings(serviceMessage.getParam("expiresForTypes"));
                website.setExpiresForTypes(expiresRaw);
                List<String> staticFileExtensions = website.getStaticFileExtensions();
                for (String ex : website.getExpiresForTypes().keySet()) {
                    if (!staticFileExtensions.contains(ex)) {
                        staticFileExtensions.add(ex);
                    }
                }
                website.setStaticFileExtensions(staticFileExtensions);
            }
        } catch (ClassCastException e) {
            log.error("WebSite update ClassCastException: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(website);
        validate(website);
        store(website);

        return website;
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfResourceArchive.dropByArchivedResourceId(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    @Override
    public WebSite buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
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

            String unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));

            CharSet charSet = null;
            String charsetAsString;
            if (serviceMessage.getParam("charSet") != null) {
                charsetAsString = cleaner.cleanString((String) serviceMessage.getParam("charSet"));
                charSet = Enum.valueOf(CharSet.class, charsetAsString);
            }

            Boolean ssiEnabled = cleaner.cleanBoolean(serviceMessage.getParam("ssiEnabled"));
            List<String> ssiFileExtensions = new ArrayList<>();
            if (serviceMessage.getParam("ssiFileExtensions") != null) {
                ssiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("ssiFileExtensions"));
            }
            Boolean cgiEnabled = cleaner.cleanBoolean(serviceMessage.getParam("cgiEnabled"));
            List<String> cgiFileExtensions = new ArrayList<>();
            if (serviceMessage.getParam("cgiFileExtensions") != null) {
                cgiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("cgiFileExtensions"));
            }
            String scriptAlias = cleaner.cleanString((String) serviceMessage.getParam("scriptAlias"));
            Boolean autoSubDomain = cleaner.cleanBoolean(serviceMessage.getParam("autoSubDomain"));
            Boolean accessByOldHttpVersion = cleaner.cleanBoolean(serviceMessage.getParam("accessByOldHttpVersion"));
            List<String> staticFileExtensions;
            if (serviceMessage.getParam("staticFileExtensions") != null) {
                staticFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("staticFileExtensions"));
            } else {
                staticFileExtensions = defaultWebSiteSettings.getStatic().getFileExtensions();
            }
            List<String> indexFileList = new ArrayList<>();
            if (serviceMessage.getParam("indexFileList") != null) {
                indexFileList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("indexFileList"));
            }
            Boolean accessLogEnabled = cleaner.cleanBoolean(serviceMessage.getParam("accessLogEnabled"));
            Boolean errorLogEnabled = cleaner.cleanBoolean(serviceMessage.getParam("errorLogEnabled"));
            Boolean allowUrlFopen = cleaner.cleanBoolean(serviceMessage.getParam("allowUrlFopen"));
            Integer mbstringFuncOverload = cleaner.cleanInteger(serviceMessage.getParam("mbstringFuncOverload"));
            Boolean displayErrors = cleaner.cleanBoolean(serviceMessage.getParam("displayErrors"));
            Boolean sessionUseTransSid = cleaner.cleanBoolean(serviceMessage.getParam("sessionUseTransSid"));
            Integer maxInputVars = cleaner.cleanInteger(serviceMessage.getParam("maxInputVars"));
            Integer opcacheMaxAcceleratedFiles = cleaner.cleanInteger(serviceMessage.getParam("opcacheMaxAcceleratedFiles"));
            Integer realpathCacheSize = cleaner.cleanInteger(serviceMessage.getParam("realpathCacheSize"));
            String requestOrder = (String) serviceMessage.getParam("requestOrder");
            Boolean allowUrlInclude = cleaner.cleanBoolean(serviceMessage.getParam("allowUrlInclude"));
            Integer opcacheRevalidateFreq = cleaner.cleanInteger(serviceMessage.getParam("opcacheRevalidateFreq"));
            Integer memoryLimit = cleaner.cleanInteger(serviceMessage.getParam("memoryLimit"));
            String mbstringInternalEncoding = (String) serviceMessage.getParam("mbstringInternalEncoding");
            Map<String, String> expiresForTypes = cleaner.cleanMapWithStrings(serviceMessage.getParam("expiresForType"));
            if (!expiresForTypes.isEmpty()) {
                List<String> newStaticFileExtensions = new ArrayList<>(staticFileExtensions);
                for (String ex : expiresForTypes.keySet()) {
                    if (!newStaticFileExtensions.contains(ex)) {
                        newStaticFileExtensions.add(ex);
                    }
                }
                staticFileExtensions = newStaticFileExtensions;
            }
            String appInstallCommands = cleaner.cleanString(serviceMessage.getParam("appInstallCommands"));
            String appLoadUrl = cleaner.cleanString(serviceMessage.getParam("appLoadUrl"));
            Map<String, String> appLoadParams = cleaner.cleanMapWithStrings(serviceMessage.getParam("appLoadUrl"));

            String pythonModule = cleaner.cleanString(serviceMessage.getParam("pythonModule"));
            List<String> staticFileDirs = cleaner.cleanListWithStrings(serviceMessage.getParam("staticFileDirs"));
            String appUpdateCommands = cleaner.cleanString(serviceMessage.getParam("appUpdateCommands"));
            String staticRoot = cleaner.cleanString(serviceMessage.getParam("staticRoot"));

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
            webSite.setAccessLogEnabled(accessLogEnabled != null ? accessLogEnabled : true);
            webSite.setErrorLogEnabled(errorLogEnabled != null ? errorLogEnabled : true);
            webSite.setAllowUrlFopen(allowUrlFopen != null ? allowUrlFopen : true);
            webSite.setMbstringFuncOverload(mbstringFuncOverload);
            webSite.setDisplayErrors(displayErrors);
            webSite.setSessionUseTransSid(sessionUseTransSid);
            webSite.setMaxInputVars(maxInputVars);
            webSite.setOpcacheMaxAcceleratedFiles(opcacheMaxAcceleratedFiles);
            webSite.setRealpathCacheSize(realpathCacheSize);
            webSite.setRequestOrder(requestOrder);
            webSite.setAllowUrlInclude(allowUrlInclude);
            webSite.setOpcacheRevalidateFreq(opcacheRevalidateFreq);
            webSite.setMemoryLimit(memoryLimit);
            webSite.setMbstringInternalEncoding(mbstringInternalEncoding);
            webSite.setExpiresForTypes(expiresForTypes);
            webSite.setAppLoadParams(appLoadParams);
            webSite.setAppLoadUrl(appLoadUrl);
            webSite.setAppInstallCommands(appInstallCommands);
            webSite.setPythonModule(pythonModule);
            webSite.setStaticFileDirs(staticFileDirs);
            webSite.setAppUpdateCommands(appUpdateCommands);
            webSite.setStaticRoot(staticRoot);
        } catch (ClassCastException | IllegalArgumentException e) {
            log.error("WebSite buildResourceFromServiceMessage ClassCastException: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            throw new ParameterValidationException("Один из параметров указан неверно");
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
            webSite.setDocumentRoot(webSite.getDomains().get(0).getName() + defaultWebSiteSettings.getDocumentRootPattern());
        }

        if (webSite.getUnixAccountId() == null || webSite.getUnixAccountId().equals("")) {
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("accountId", webSite.getAccountId());
            List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
            if (unixAccounts == null || unixAccounts.isEmpty()) {
                throw new ParameterValidationException("Для создания WebSite необходим UnixAccount");
            }
            webSite.setUnixAccount(unixAccounts.get(0));
        } else {
            webSite.setUnixAccount(governorOfUnixAccount.build(webSite.getUnixAccountId()));
        }

        if (webSite.getServiceId() == null || (webSite.getServiceId().equals(""))) {
            throw new ParameterValidationException("Для создания WebSite необходим ServiceId");
        }

        if (webSite.getCharSet() == null) {
            webSite.setCharSet(defaultWebSiteSettings.getCharset());
        }

        if (webSite.getSsiEnabled() == null) {
            webSite.setSsiEnabled(defaultWebSiteSettings.getSsi().getEnabled());
        }

        if (webSite.getSsiFileExtensions() == null || webSite.getSsiFileExtensions().isEmpty()) {
            webSite.setSsiFileExtensions(defaultWebSiteSettings.getSsi().getFileExtensions());
        }

        if (webSite.getCgiEnabled() == null) {
            webSite.setCgiEnabled(defaultWebSiteSettings.getCgi().getEnabled());
        }

        if (webSite.getCgiFileExtensions() == null || webSite.getCgiFileExtensions().isEmpty()) {
            webSite.setCgiFileExtensions(defaultWebSiteSettings.getCgi().getFileExtensions());
        }

        if (webSite.getScriptAlias() == null || webSite.getScriptAlias().equals("")) {
            webSite.setScriptAlias(defaultWebSiteSettings.getScriptAlias());
        }

        if (webSite.getDdosProtection() == null) {
            webSite.setDdosProtection(defaultWebSiteSettings.getDdosProtection());
        }

        if (webSite.getAutoSubDomain() == null) {
            webSite.setAutoSubDomain(defaultWebSiteSettings.getAutoSubDomain());
        }

        if (webSite.getAccessByOldHttpVersion() == null) {
            webSite.setAccessByOldHttpVersion(defaultWebSiteSettings.getAccessByOldHttpVersion());
        }

        if (webSite.getStaticFileExtensions() == null) {
            webSite.setStaticFileExtensions(defaultWebSiteSettings.getStatic().getFileExtensions());
        }

        if (webSite.getCustomUserConf() == null) {
            webSite.setCustomUserConf(defaultWebSiteSettings.getCustomUserConf());
        }

        if (webSite.getIndexFileList() == null || webSite.getIndexFileList().isEmpty()) {
            webSite.setIndexFileList(defaultWebSiteSettings.getIndexFileList());
        }

        if (webSite.getAccessLogEnabled() == null) {
            webSite.setAccessLogEnabled(defaultWebSiteSettings.getAccessLogEnabled());
        }

        if (webSite.getErrorLogEnabled() == null) {
            webSite.setErrorLogEnabled(defaultWebSiteSettings.getErrorLogEnabled());
        }

        if (webSite.getMbstringFuncOverload() == null || webSite.getMbstringFuncOverload() < 0 || webSite.getMbstringFuncOverload() > 7) {
            webSite.setMbstringFuncOverload(defaultWebSiteSettings.getMbstringFuncOverload());
        }

        if (webSite.getAllowUrlFopen() == null) {
            webSite.setAllowUrlFopen(defaultWebSiteSettings.getAllowUrlFopen());
        }

        if (webSite.getFollowSymLinks() == null) {
            webSite.setFollowSymLinks(defaultWebSiteSettings.getFollowSymLinks());
        }

        if (webSite.getMultiViews() == null) {
            webSite.setMultiViews(defaultWebSiteSettings.getMultiViews());
        }
        if (webSite.getExpiresForTypes() == null) {
            webSite.setExpiresForTypes(new HashMap<>());
        }
        if (webSite.getStaticFileDirs() == null) {
            webSite.setStaticFileDirs(new ArrayList<>());
        }
        if (webSite.getStaticRoot() == null) {
            webSite.setStaticRoot("");
        }

        Map<String, String> expiresForTypes = webSite.getExpiresForTypes().entrySet().stream()
                .filter(extExpires -> !"off".equals(extExpires.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        webSite.setExpiresForTypes(expiresForTypes);
    }

    @Override
    public void validate(WebSite webSite) throws ParameterValidationException {
        Set<ConstraintViolation<WebSite>> constraintViolations = validator.validate(webSite, WebSiteChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("webSite: " + webSite + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(WebSite webSite) {
        Set<ConstraintViolation<WebSite>> constraintViolations = validator.validate(webSite, WebSiteImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] webSite: " + webSite + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected WebSite construct(WebSite webSite) throws ParameterValidationException {
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
        WebSite webSite = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Сайт с ID " + resourceId + " не найден"));

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
        List<WebSite> webSites;
        if (keyValue.get("unixAccountId") != null) {
            webSites = repository.findByUnixAccountId(keyValue.get("unixAccountId"));
        } else if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            webSites = repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId"));
        } else if (keyValue.get("accountId") != null) {
            webSites = repository.findByAccountId(keyValue.get("accountId"));
        } else if (keyValue.get("serviceId") != null) {
            webSites = repository.findByServiceId(keyValue.get("serviceId"));
        } else {
             return Collections.emptyList();
        }

        return MapUtils.getBooleanValue(keyValue, "withoutBuiltIn") ?
                webSites : webSites.stream().map(this::construct).collect(Collectors.toList());
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
        return new Count(repository.countByAccountId(accountId));
    }

}
