package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.resources.validation.*;

@Document(collection = "webSites")
@ValidWebSite
public class WebSite extends Resource implements Serviceable {

    @Transient
    @NotNull(message = "unixAccount не может быть null")
    private UnixAccount unixAccount;

    @NotNull(message = "unixAccountId не может быть null")
    private String unixAccountId;

    @ServiceId
    @NotNull(message = "serviceId не может быть null")
    private String serviceId;

    @NotBlank(message = "documentRoot не может быть пустым")
    @ValidRelativeFilePath
    private String documentRoot;

    @Transient
    @NotEmpty(message = "Должен присутствовать хотя бы один домен")
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

    @Valid
    private List<@ValidFileExtension String> staticFileExtensions = new ArrayList<>();

    private String customUserConf;

    @Valid
    private List<@ValidFileName String> indexFileList = new ArrayList<>();

    private Boolean accessLogEnabled;

    private Boolean errorLogEnabled;

    private Boolean followSymLinks;

    private Boolean multiViews;

    private Boolean allowUrlFopen;

    @Range(min = 0, max = 7, message = "mbstringFuncOverload должно быть между {min} и {max}")
    private Integer mbstringFuncOverload;

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
                '}';
    }
}
