package ru.majordomo.hms.rc.user.resources;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.Resource;
import ru.majordomo.hms.rc.user.common.CharSet;

@Document(collection = "webSites")
public class WebSite extends Resource {
    private Resource unixAccount;
    private String applicationServer;
    private String documentRoot;
    private List<Resource> domains = new ArrayList<>();

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

    public Resource getUnixAccount() {
        return unixAccount;
    }

    public void setUnixAccount(Resource unixAccount) {
        this.unixAccount = unixAccount;
    }

    public String getApplicationServer() {
        return applicationServer;
    }

    public void setApplicationServer(String applicationServer) {
        this.applicationServer = applicationServer;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public void setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    public List<Resource> getDomains() {
        return domains;
    }

    public void setDomains(List<Resource> domains) {
        this.domains = domains;
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
                ", applicationServer='" + applicationServer + '\'' +
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
