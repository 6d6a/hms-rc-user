package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "mailboxes")
public class Mailbox extends Resource {
    private Resource domain;
    private List<String> blackList = new ArrayList<>();
    private List<String> whiteList = new ArrayList<>();
    private Long quota;
    private Long size;
    private Boolean antiSpamEnabled = false;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Resource getDomain() {
        return domain;
    }

    public void setDomain(Resource domain) {
        this.domain = domain;
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
}
