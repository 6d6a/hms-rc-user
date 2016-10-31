package ru.majordomo.hms.rc.user.resources;

import java.util.ArrayList;
import java.util.List;

public class Passport {
    private String number;
    private String issuedOrg;
    private Long issuedDate;
    private Long birthday;
    private List<String> pages = new ArrayList<>();

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

    public Long getHumanReadableIssuedDate() {

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

    public List<String> getPages() {
        return pages;
    }

    public void setPages(List<String> pages) {
        this.pages = pages;
    }

    public void addPage(String pageUrl) {
        this.pages.add(pageUrl);
    }

    @Override
    public String toString() {
        return "Passport{" +
                "number='" + number + '\'' +
                ", issuedOrg='" + issuedOrg + '\'' +
                ", issuedDate=" + issuedDate +
                ", birthday=" + birthday +
                ", pages=" + pages +
                '}';
    }
}
