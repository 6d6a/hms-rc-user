package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import ru.majordomo.hms.rc.user.resources.validation.LocalDatePast;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualForeignChecks;

public class Passport {
    @NotBlank(groups = {PersonIndividualForeignChecks.class, PersonEntrepreneurForeignChecks.class})
    @Length(min = 10, max = 255, groups = {PersonIndividualForeignChecks.class, PersonEntrepreneurForeignChecks.class})
    @Pattern(regexp = "(?ui)(^([a-zа-яё0-9\\,\\.\\/ -]+)$)", groups = {PersonIndividualForeignChecks.class, PersonEntrepreneurForeignChecks.class})
    private String document;

    @NotBlank(groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    @Length(min = 10, max = 10, groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(regexp = "(^[0-9]+$)", groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    private String number;

    @NotBlank(groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    @Length(min = 10, max = 200, groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(regexp = "(?ui)(^[а-яё№\\(\\)\\d\\.\\,\\/ -]+$)", groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    private String issuedOrg;

    @NotNull(groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class})
    private LocalDate issuedDate;

    @NotNull(groups = {
            PersonIndividualChecks.class,
            PersonIndividualForeignChecks.class,
            PersonEntrepreneurChecks.class,
            PersonEntrepreneurForeignChecks.class
    })
    @LocalDatePast(groups = {
            PersonIndividualChecks.class,
            PersonIndividualForeignChecks.class,
            PersonEntrepreneurChecks.class,
            PersonEntrepreneurForeignChecks.class
    })
    private LocalDate birthday;

    private String mainPage;
    private String registerPage;

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
        if (issuedDate != null) {
            return issuedDate.toString();
        }
        return null;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public void setIssuedDate(String date) {
        if (date != null) {
            issuedDate = LocalDate.parse(date);
        }
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    @JsonGetter("birthday")
    public String getBirthdayAsString() {
        if (birthday != null) {
            return birthday.toString();
        }
        return null;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public void setBirthday(String date) {
        if (date != null) {
            birthday = LocalDate.parse(date);
        }
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "Passport{" +
                "document='" + document + '\'' +
                ", number='" + number + '\'' +
                ", issuedOrg='" + issuedOrg + '\'' +
                ", issuedDate=" + issuedDate +
                ", birthday=" + birthday +
                ", mainPage='" + mainPage + '\'' +
                ", registerPage='" + registerPage + '\'' +
                '}';
    }

        @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passport passport = (Passport) o;
        return Objects.equals(number, passport.number) &&
                Objects.equals(document, passport.document) &&
                Objects.equals(issuedOrg, passport.issuedOrg) &&
                Objects.equals(issuedDate, passport.issuedDate) &&
                Objects.equals(birthday, passport.birthday) &&
                Objects.equals(mainPage, passport.mainPage) &&
                Objects.equals(registerPage, passport.registerPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, issuedOrg, issuedDate, birthday, mainPage, registerPage);
    }
}
