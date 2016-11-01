package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Passport {
    private String number;
    private String issuedOrg;
    private LocalDate issuedDate;
    private LocalDate birthday;
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

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    @JsonGetter("issuedDate")
    public String getIssuedDateAsString() {
        return issuedDate.toString();
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public void setIssuedDate(String date) {
        issuedDate = LocalDate.parse(date);
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    @JsonGetter("birthday")
    public String getBirthdayAsString() {
        return birthday.toString();
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public void setBirthday(String date) {
        birthday = LocalDate.parse(date);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passport passport = (Passport) o;
        return Objects.equals(number, passport.number) &&
                Objects.equals(issuedOrg, passport.issuedOrg) &&
                Objects.equals(issuedDate, passport.issuedDate) &&
                Objects.equals(birthday, passport.birthday) &&
                Objects.equals(pages, passport.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, issuedOrg, issuedDate, birthday, pages);
    }
}
