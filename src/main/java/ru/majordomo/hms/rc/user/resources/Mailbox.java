package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "mailboxes")
public class Mailbox extends Resource {
    @Transient
    private Domain domain;
    private String domainId;
    private List<String> blackList;
    private List<String> whiteList;
    private Long quota;
    private Long size;
    private Boolean antiSpamEnabled = false;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
        setDomainId(domain.getId());
    }

    public List<String> getBlackList() {
        return blackList;
    }

    public void setBlackList(List<String> blackList) {
        this.blackList = blackList;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public Long getQuota() {
        return quota;
    }

    public void setQuota(Long quota) {
        this.quota = quota;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public void setAntiSpamEnabled(Boolean antiSpamEnabled) {
        this.antiSpamEnabled = antiSpamEnabled;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
