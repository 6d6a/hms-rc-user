package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.io.File;
import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "passports")
public class Passport extends Resource {
    private String number;
    private String issuedOrg;
    private Long issuedDate;
    private Long birthday;
    private List<File> pages;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getIssuedOrg() {
        return issuedOrg;
    }

    public void setIssuedOrg(String issuedOrg) {
        this.issuedOrg = issuedOrg;
    }

    public Long getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Long issuedDate) {
        this.issuedDate = issuedDate;
    }

    public Long getBirthday() {
        return birthday;
    }

    public void setBirthday(Long birthday) {
        this.birthday = birthday;
    }

    public List<File> getPages() {
        return pages;
    }

    public void setPages(List<File> pages) {
        this.pages = pages;
    }
}
