package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.common.CharSet;

@Document(collection = "webSites")
public class WebSite extends Resource implements ServerStorable {


    @Transient
    private UnixAccount unixAccount;
    private String unixAccountId;
    private String serverId;
    private String documentRoot;
    private String serviceId;

    @Transient
    private List<Domain> domains = new ArrayList<>();
    private List<String> domainIds = new ArrayList<>();
    private CharSet charSet;
    private Boolean ssiEnabled;
    private List<String> ssiFileExtensions = new ArrayList<>();
    private Boolean cgiEnabled;
    private List<String> cgiFileExtensions = new ArrayList<>();
    private String scriptAlias;
    private Boolean ddosProtection;
    private Boolean autoSubDomain;
    private Boolean accessByOldHttpVersion;
    private List<String> staticFileExtensions = new ArrayList<>();
    private String customUserConf;
    private List<String> indexFileList = new ArrayList<>();
    private Boolean accessLogEnabled;
    private Boolean errorLogEnabled;
    public CharSet getCharSet() {
        return charSet;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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
        this.unixAccountId = unixAccount.getId();
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    @Override
    public void setServerId(String serverId) {
        this.serverId = serverId;
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
        this.domains.add(domain);
    }

    @Override
    public String toString() {
        return "WebSite{" +
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", unixAccount=" + unixAccount +
                ", serverId='" + serverId + '\'' +
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
