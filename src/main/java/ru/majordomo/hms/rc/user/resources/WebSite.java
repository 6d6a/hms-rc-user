package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.resources.validation.*;
import ru.majordomo.hms.rc.user.resources.validation.group.WebSiteChecks;

@Document(collection = "webSites")
@ValidWebSite
@JsonFilter("websiteFilter")
public class WebSite extends Resource implements Serviceable {

    @Transient
    private UnixAccount unixAccount;

    @NotNull(message = "unixAccountId не может быть null")
    @Indexed
    private String unixAccountId;

    @ServiceId(groups = WebSiteChecks.class)
    @NotNull(message = "serviceId не может быть null")
    @Indexed
    private String serviceId;   // ru.majordomo.hms.rc.staff.resources.Service

    @NotBlank(message = "documentRoot не может быть пустым")
    @ValidRelativeFilePath
    private String documentRoot;

    @Transient
    private List<Domain> domains = new ArrayList<>();

    @NotEmpty(message = "Должен присутствовать хотя бы один id домена")
    @ObjectIdCollection(Domain.class)
    private List<String> domainIds = new ArrayList<>();

    @NotNull(message = "charSet не может быть null")
    private CharSet charSet;

    private Boolean ssiEnabled;

    @Valid
    private List<@ValidFileExtension String> ssiFileExtensions = new ArrayList<>();

    private Boolean cgiEnabled;

    @Valid
    private List<@ValidFileExtension String> cgiFileExtensions = new ArrayList<>();

    @ValidRelativeFilePath
    private String scriptAlias;

    private Boolean ddosProtection;

    private Boolean autoSubDomain;

    private Boolean accessByOldHttpVersion;

    /**
     * Список файлов которые будут обрабатываться nginx. Запросы к этим файлам не будут передаваться на бэкэнд web-сервер
     */
    @Valid
    private List<@ValidFileExtension String> staticFileExtensions = new ArrayList<>();

    private String customUserConf;

    /**
     * Команды выполняемые во время установки пользовательского приложения
     */
    @Nullable
    private String appInstallCommands;

    @Nullable
    public String getAppInstallCommands() {
        return appInstallCommands;
    }

    public void setAppInstallCommands(@Nullable String appInstallCommands) {
        this.appInstallCommands = appInstallCommands;
    }

    @Nullable
    public String getAppLoadUrl() {
        return appLoadUrl;
    }

    public void setAppLoadUrl(@Nullable String appLoadUrl) {
        this.appLoadUrl = appLoadUrl;
    }

    public Map<String, String> getAppLoadParams() {
        return appLoadParams;
    }

    public void setAppLoadParams(Map<String, String> appLoadParams) {
        this.appLoadParams = appLoadParams;
    }

    /**
     * Адрес по которому нужно загрузить приложение
     */
    @Nullable
    private String appLoadUrl;

    /**
     * Дополнительные параметры необходимые для загрузки приложения из репозитория клиента.
     * Например: имя пользователя, пароль, ветка git
     */
    private Map<String, String> appLoadParams = new HashMap<>();

    @Valid
    private List<@ValidFileName String> indexFileList = new ArrayList<>();

    private Boolean accessLogEnabled;

    private Boolean errorLogEnabled;

    private Boolean followSymLinks;

    private Boolean multiViews;

    private Boolean allowUrlFopen;

    /**
     * Содержит список расширений для которых нужно добавить опцию nginx expires и значение.
     * значение - понимаемое nginx значение параметра expires
     * Все эти типы так же должны быть перечислены в staticFileExtensions
     *
     * желательно удалять значения off для упрощения конфига nginx
     */
    @Valid
    private Map<@ValidFileExtension String, @ValidExpires String> expiresForTypes = new HashMap<>();    // значение опции nginx expires для отдельных расширений

    @Range(min = 0, max = 7, message = "mbstringFuncOverload должно быть между {min} и {max}")
    private Integer mbstringFuncOverload;

    private Boolean displayErrors;

    private Boolean sessionUseTransSid;

    private Integer maxInputVars;

    private Integer opcacheMaxAcceleratedFiles;

    private Integer realpathCacheSize;

    private String requestOrder;

    private Boolean allowUrlInclude;

    private Integer opcacheRevalidateFreq;

    private Integer memoryLimit;

    private String mbstringInternalEncoding;

    private List<String> resourceFilter = new ArrayList<>();

    public List<String> getResourceFilter() {
        return resourceFilter;
    }

    public List<String> getFilteredFieldsAsSequence() {
        List<String> filtered = new ArrayList<>();
        filtered.add("id");
        filtered.add("name");
        filtered.add("accountId");
        filtered.add("switchedOn");
        filtered.add("lockedDateTime");
        filtered.add("willBeDeletedAfter");
        filtered.add("unixAccount");
        filtered.add("unixAccountId");
        filtered.add("serviceId");
        filtered.add("documentRoot");
        filtered.add("domains");
        filtered.add("domainIds");
        filtered.add("charSet");
        if (resourceFilter != null) {
            filtered.addAll(resourceFilter);
        }
        return filtered;
    }

    public void setResourceFilter(List<String> resourceFilter) {
        this.resourceFilter = resourceFilter;
    }

    public Boolean getFollowSymLinks() {
        return followSymLinks;
    }

    public void setFollowSymLinks(Boolean followSymLinks) {
        this.followSymLinks = followSymLinks;
    }

    public Boolean getMultiViews() {
        return multiViews;
    }

    public void setMultiViews(Boolean multiViews) {
        this.multiViews = multiViews;
    }

    public Boolean getAllowUrlFopen() {
        return allowUrlFopen;
    }

    public void setAllowUrlFopen(Boolean allowUrlFopen) {
        this.allowUrlFopen = allowUrlFopen;
    }

    public Integer getMbstringFuncOverload() {
        return mbstringFuncOverload;
    }

    public void setMbstringFuncOverload(Integer mbstringFuncOverload) {
        this.mbstringFuncOverload = mbstringFuncOverload;
    }

    public CharSet getCharSet() {
        return charSet;
    }

    public void setCharSet(CharSet charSet) {
        this.charSet = charSet;
    }

    public Boolean getSsiEnabled() {
        return ssiEnabled;
    }

    public void setSsiEnabled(Boolean ssiEnabled) {
        this.ssiEnabled = ssiEnabled;
    }

    public List<String> getSsiFileExtensions() {
        return ssiFileExtensions;
    }

    public void setSsiFileExtensions(List<String> ssiFileExtensions) {
        this.ssiFileExtensions = ssiFileExtensions;
    }

    public Boolean getCgiEnabled() {
        return cgiEnabled;
    }

    public void setCgiEnabled(Boolean cgiEnabled) {
        this.cgiEnabled = cgiEnabled;
    }

    public List<String> getCgiFileExtensions() {
        return cgiFileExtensions;
    }

    public void setCgiFileExtensions(List<String> cgiFileExtensions) {
        this.cgiFileExtensions = cgiFileExtensions;
    }

    public String getScriptAlias() {
        return scriptAlias;
    }

    public void setScriptAlias(String scriptAlias) {
        this.scriptAlias = scriptAlias;
    }

    public Boolean getDdosProtection() {
        return ddosProtection;
    }

    public void setDdosProtection(Boolean ddosProtection) {
        this.ddosProtection = ddosProtection;
    }

    public Boolean getAutoSubDomain() {
        return autoSubDomain;
    }

    public void setAutoSubDomain(Boolean autoSubDomain) {
        this.autoSubDomain = autoSubDomain;
    }

    public Boolean getAccessByOldHttpVersion() {
        return accessByOldHttpVersion;
    }

    public void setAccessByOldHttpVersion(Boolean accessByOldHttpVersion) {
        this.accessByOldHttpVersion = accessByOldHttpVersion;
    }

    public List<String> getStaticFileExtensions() {
        return staticFileExtensions;
    }

    public void setStaticFileExtensions(List<String> staticFileExtensions) {
        this.staticFileExtensions = staticFileExtensions;
    }

    public String getCustomUserConf() {
        return customUserConf;
    }

    public void setCustomUserConf(String customUserConf) {
        this.customUserConf = customUserConf;
    }

    public List<String> getIndexFileList() {
        return indexFileList;
    }

    public void setIndexFileList(List<String> indexFileList) {
        this.indexFileList = indexFileList;
    }

    public Boolean getAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(Boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }

    public Boolean getErrorLogEnabled() {
        return errorLogEnabled;
    }

    public void setErrorLogEnabled(Boolean errorLogEnabled) {
        this.errorLogEnabled = errorLogEnabled;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Map<String, String> getExpiresForTypes() {
        return expiresForTypes;
    }

    public void setExpiresForTypes(Map<String, String> expiresForTypes) {
        this.expiresForTypes = expiresForTypes;
    }

    public UnixAccount getUnixAccount() {
        return unixAccount;
    }

    public void setUnixAccount(UnixAccount unixAccount) {
        this.unixAccount = unixAccount;
        if (unixAccount == null) {
            this.unixAccountId = null;
        } else {
            this.unixAccountId = unixAccount.getId();
        }
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public void setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    public List<Domain> getDomains() {
        return domains;
    }

    public void setDomains(List<Domain> domains) {
        this.domains = domains;
        for (Domain domain: domains) {
            domainIds.add(domain.getId());
        }
    }

    @JsonIgnore
    public String getUnixAccountId() {
        return unixAccountId;
    }

    public void setUnixAccountId(String unixAccountId) {
        this.unixAccountId = unixAccountId;
    }

    @JsonIgnore
    public List<String> getDomainIds() {
        return domainIds;
    }

    public void setDomainIds(List<String> domainIds) {
        this.domainIds = domainIds;
    }

    public void addDomain(Domain domain) {
        if (domain != null && !this.domains.contains(domain)) {
            if (domain.getId() != null && !this.domainIds.contains(domain.getId())) {
                this.domainIds.add(domain.getId());
            }
            this.domains.add(domain);
        }
    }


    public Boolean getDisplayErrors() {
        return displayErrors;
    }

    public void setDisplayErrors(Boolean displayErrors) {
        this.displayErrors = displayErrors;
    }

    public Boolean getSessionUseTransSid() {
        return sessionUseTransSid;
    }

    public void setSessionUseTransSid(Boolean sessionUseTransSid) {
        this.sessionUseTransSid = sessionUseTransSid;
    }

    public Integer getMaxInputVars() {
        return maxInputVars;
    }

    public void setMaxInputVars(Integer maxInputVars) {
        this.maxInputVars = maxInputVars;
    }

    public Integer getOpcacheMaxAcceleratedFiles() {
        return opcacheMaxAcceleratedFiles;
    }

    public void setOpcacheMaxAcceleratedFiles(Integer opcacheMaxAcceleratedFiles) {
        this.opcacheMaxAcceleratedFiles = opcacheMaxAcceleratedFiles;
    }

    public Integer getRealpathCacheSize() {
        return realpathCacheSize;
    }

    public void setRealpathCacheSize(Integer realpathCacheSize) {
        this.realpathCacheSize = realpathCacheSize;
    }

    public String getRequestOrder() {
        return requestOrder;
    }

    public void setRequestOrder(String requestOrder) {
        this.requestOrder = requestOrder;
    }

    public Boolean getAllowUrlInclude() {
        return allowUrlInclude;
    }

    public void setAllowUrlInclude(Boolean allowUrlInclude) {
        this.allowUrlInclude = allowUrlInclude;
    }

    public Integer getOpcacheRevalidateFreq() {
        return opcacheRevalidateFreq;
    }

    public void setOpcacheRevalidateFreq(Integer opcacheRevalidateFreq) {
        this.opcacheRevalidateFreq = opcacheRevalidateFreq;
    }

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getMbstringInternalEncoding() {
        return mbstringInternalEncoding;
    }

    public void setMbstringInternalEncoding(String mbstringInternalEncoding) {
        this.mbstringInternalEncoding = mbstringInternalEncoding;
    }

    @Override
    public String toString() {
        return "WebSite{" +
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", unixAccount=" + unixAccount +
                ", serviceId='" + serviceId + '\'' +
                ", documentRoot='" + documentRoot + '\'' +
                ", domains=" + domains +
                ", charSet=" + charSet +
                ", ssiEnabled=" + ssiEnabled +
                ", ssiFileExtensions=" + ssiFileExtensions +
                ", cgiEnabled=" + cgiEnabled +
                ", cgiFileExtensions=" + cgiFileExtensions +
                ", scriptAlias='" + scriptAlias + '\'' +
                ", ddosProtection=" + ddosProtection +
                ", autoSubDomain=" + autoSubDomain +
                ", accessByOldHttpVersion=" + accessByOldHttpVersion +
                ", staticFileExtensions=" + staticFileExtensions +
                ", customUserConf='" + customUserConf + '\'' +
                ", indexFileList=" + indexFileList +
                ", accessLogEnabled=" + accessLogEnabled +
                ", errorLogEnabled=" + errorLogEnabled +
                ", followSymLinks=" + followSymLinks +
                ", multiViews=" + multiViews +
                ", allowUrlFopen=" + allowUrlFopen +
                ", mbstringFuncOverload=" + mbstringFuncOverload +
                ", displayErrors=" + displayErrors +
                ", sessionUseTransSid=" + sessionUseTransSid +
                ", maxInputVars=" + maxInputVars +
                ", opcacheMaxAcceleratedFiles=" + opcacheMaxAcceleratedFiles +
                ", realpathCacheSize=" + realpathCacheSize +
                ", requestOrder='" + requestOrder + '\'' +
                ", allowUrlInclude=" + allowUrlInclude +
                ", opcacheRevalidateFreq=" + opcacheRevalidateFreq +
                ", memoryLimit=" + memoryLimit +
                ", mbstringInternalEncoding=" + mbstringInternalEncoding +
                ", expiresForTypes=" + expiresForTypes +
                "} " + super.toString();
    }
}
