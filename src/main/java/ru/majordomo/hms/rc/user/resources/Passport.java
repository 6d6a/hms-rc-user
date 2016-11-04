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
    private String mainPage;
    private String registerPage;
    private String address;

    public String getMainPage() {
        return mainPage;
    }

    public void setMainPage(String mainPage) {
        this.mainPage = mainPage;
    }

    public String getRegisterPage() {
        return registerPage;
    }

    public void setRegisterPage(String registerPage) {
        this.registerPage = registerPage;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    @JsonGetter("issuedDate")
    public String getIssuedDateAsString() {
        if (issuedDate != null)
            return issuedDate.toString();
        return null;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public void setIssuedDate(String date) {
        if (date != null)
            issuedDate = LocalDate.parse(date);
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    @JsonGetter("birthday")
    public String getBirthdayAsString() {
        if (birthday != null)
            return birthday.toString();
        return null;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public void setBirthday(String date) {
        if (birthday != null)
            birthday = LocalDate.parse(date);
    }

    @Override
    public String toString() {
        return "Passport{" +
                "number='" + number + '\'' +
                ", issuedOrg='" + issuedOrg + '\'' +
                ", issuedDate=" + issuedDate +
                ", birthday=" + birthday +
                ", mainPage='" + mainPage + '\'' +
                ", registerPage='" + registerPage + '\'' +
                ", address='" + address + '\'' +
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
                Objects.equals(mainPage, passport.mainPage) &&
                Objects.equals(registerPage, passport.registerPage) &&
                Objects.equals(address, passport.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, issuedOrg, issuedDate, birthday, mainPage, registerPage, address);
    }
}
