package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;

import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;

import ru.majordomo.hms.rc.user.resources.validation.LocalDatePast;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualForeignChecks;

public class Passport {
    @NotBlank(
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Документ' обязательно для заполнения"
    )
    @Length(
            min = 10,
            max = 255,
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Документ' должно содержать от {min} до {max} символов"
    )
    @Pattern(
            regexp = "(?ui)(^([a-zа-яё0-9\\,\\.\\/ -]+)$)",
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "В поле 'Документ' разрешены только символы русского и латинского алфавита, цифры, а также ',', '.', '/' и '-'"
    )
    @Null(
            message = "Поле 'Документ' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private String document;

    @NotBlank(
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "Поле 'Номер паспорта' обязательно для заполнения"
    )
    @Length(
            min = 10,
            max = 10,
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "Поле 'Номер паспорта' должно содержать ровно {max} символов"
    )
    @Pattern(
            regexp = "(^[0-9]+$)",
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "В поле 'Номер паспорта' разрешены только цифры"
    )
    @Null(
            message = "Поле 'Номер паспорта' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            }
    )
    private String number;

    @NotBlank(
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "Поле 'Паспорт выдан' обязательно для заполнения"
    )
    @Length(
            min = 10,
            max = 200,
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "Поле 'Паспорт выдан' должно содержать от {min} до {max} символов"
    )
    @Pattern(
            regexp = "(?ui)(^[а-яё№\\(\\)\\d\\.\\,\\/ -]+$)",
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "В поле 'Паспорт выдан' разрешены только символы русского алфавита, цифры, а также '№', '(', ')', ',', '.', '/' и '-'"
    )
    @Null(
            message = "Поле 'Паспорт выдан' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            }
    )
    private String issuedOrg;

    @NotNull(
            groups = {
                    PersonIndividualChecks.class,
                    PersonEntrepreneurChecks.class
            },
            message = "Поле 'Дата выдачи паспорта' обязательно для заполнения"
    )
    @Null(
            message = "Поле 'Дата выдачи паспорта' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurForeignChecks.class
            }
    )
    private LocalDate issuedDate;

    @NotNull(
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Дата рождения' обязательно для заполнения"
    )
    @LocalDatePast(
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Дата рождения' должно быть ранее текущей даты"
    )
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
