package ru.majordomo.hms.rc.user.resources;

import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotBlank;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.groups.ConvertGroup;

import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyForeignLegalAddressChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyLegalAddressChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurForeignChecks;

public class LegalEntity {
    @NotBlank(
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'ИНН' обязательно для заполнения"
    )
    @Length.List(
            {
                    @Length(
                            min = 10,
                            max = 10,
                            groups = {PersonCompanyChecks.class},
                            message = "Поле 'ИНН' должно содержать ровно {max} символов"
                    ),
                    @Length(
                            min = 12,
                            max = 12,
                            groups = {PersonEntrepreneurChecks.class},
                            message = "Поле 'ИНН' должно содержать ровно {max} символов"
                    )
            }
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'ИНН' разрешены только цифры"
    )
    @Null(
            message = "Поле 'ИНН' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class
            }
    )
    private String inn;

    @NotBlank(
            groups = {PersonCompanyChecks.class},
            message = "Поле 'КПП' обязательно для заполнения"
    )
    @Length(
            min = 9,
            max = 9,
            groups = {PersonCompanyChecks.class},
            message = "Поле 'КПП' должно содержать ровно {max} символов"
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class},
            message = "В поле 'КПП' разрешены только цифры"
    )
    @Null(
            message = "Поле 'КПП' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private String kpp;

    @NotBlank(
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'ОГРН' обязательно для заполнения"
    )
    @Length.List(
            {
                    @Length(
                            min = 13,
                            max = 13,
                            groups = {PersonCompanyChecks.class},
                            message = "Поле 'ОГРН' должно содержать ровно {max} символов"
                    ),
                    @Length(
                            min = 15,
                            max = 15,
                            groups = {PersonEntrepreneurChecks.class},
                            message = "Поле 'ОГРН' должно содержать ровно {max} символов"
                    )
            }
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'ОГРН' разрешены только цифры"
    )
    @Null(
            message = "Поле 'ОГРН' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class
            }
    )
    private String ogrn;

    @Valid
    @NotNull(
            message = "Юридический адрес должен быть заполнен",
            groups = {
                    PersonCompanyChecks.class
            }
    )
    @ConvertGroup.List(
            {
                    @ConvertGroup(from = PersonCompanyChecks.class, to = PersonCompanyLegalAddressChecks.class),
                    @ConvertGroup(from = PersonCompanyForeignChecks.class, to = PersonCompanyForeignLegalAddressChecks.class)
            }
    )
    @Null(
            message = "Поле 'Юридический адрес' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private Address address;

    @Length(
            max = 128,
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'Наименование банка' должно содержать не более {max} символов"
    )
    @Pattern(
            regexp = "(?ui)(^([а-яё0-9\\,\\.\"\\(\\)№ -]+)$|^([a-z0-9\\,\\.\"\\(\\)# -]+)$|^$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'Наименование банка' разрешены символы только русского алфавита или " +
                    "только латинского алфавита, цифры, а также '№', '#', '(', ')', '\"', ',', '.' и '-'"
    )
    @Null(
            message = "Поле 'Наименование банка' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonCompanyForeignChecks.class
            }
    )
    private String bankName;

    @Length(
            min = 9,
            max = 9,
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'БИК' должно содержать ровно {max} символов"
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'БИК' разрешены только цифры"
    )
    @Null(
            message = "Поле 'БИК' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonCompanyForeignChecks.class
            }
    )
    private String bik;

    @Length(
            min = 20,
            max = 20,
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'Кор. счет' должно содержать ровно {max} символов"
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'Кор. счет' разрешены только цифры"
    )
    @Null(
            message = "Поле 'Кор. счет' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonCompanyForeignChecks.class
            }
    )
    private String correspondentAccount;

    @Length(
            min = 20,
            max = 20,
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "Поле 'Счет' должно содержать ровно {max} символов"
    )
    @Pattern(
            regexp = "(^[\\d]+$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class},
            message = "В поле 'Счет' разрешены только цифры"
    )
    @Null(
            message = "Поле 'Счет' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonCompanyForeignChecks.class
            }
    )
    private String bankAccount;

    @NotBlank(
            groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class},
            message = "Поле 'Имя директора' обязательно для заполнения"
    )
    @Length(
            min = 2,
            max = 64,
            groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class},
            message = "Поле 'Имя директора' должно содержать от {min} до {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё]+$)",
                            groups = {PersonCompanyChecks.class},
                            message = "В поле 'Имя директора' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$)",
                            groups = {PersonCompanyForeignChecks.class},
                            message = "В поле 'Имя директора' разрешены символы только русского алфавита или только латинского алфавита"
                    )
            }
    )
    @Null(
            message = "Поле 'Имя директора' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private String directorFirstname;

    @NotBlank(
            groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class},
            message = "Поле 'Фамилия директора' обязательно для заполнения"
    )
    @Length(
            min = 2,
            max = 64,
            groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class},
            message = "Поле 'Фамилия директора' должно содержать от {min} до {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё-]+$)",
                            groups = {PersonCompanyChecks.class},
                            message = "В поле 'Фамилия директора' разрешены только символы русского алфавита и дефис"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё-]+)$|^([a-z-]+)$)",
                            groups = {PersonCompanyForeignChecks.class},
                            message = "В поле 'Фамилия директора' разрешены символы только русского алфавита или только латинского алфавита и дефис"
                    )
            }
    )
    @Null(
            message = "Поле 'Фамилия директора' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private String directorLastname;

    @Length(
            max = 64,
            groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class},
            message = "Поле 'Отчество директора' должно содержать не более {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё]+$|^$)",
                            groups = {PersonCompanyChecks.class},
                            message = "В поле 'Отчество директора' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$|^$)",
                            groups = {PersonCompanyForeignChecks.class},
                            message = "В поле 'Отчество директора' разрешены символы только русского алфавита или только латинского алфавита"
                    )
            }
    )
    @Null(
            message = "Поле 'Отчество директора' нельзя заполнять для выбранного типа персоны",
            groups = {
                    PersonEntrepreneurForeignChecks.class,
                    PersonEntrepreneurChecks.class
            }
    )
    private String directorMiddlename;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getKpp() {
        return kpp;
    }

    public void setKpp(String kpp) {
        this.kpp = kpp;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBik() {
        return bik;
    }

    public void setBik(String bik) {
        this.bik = bik;
    }

    public String getCorrespondentAccount() {
        return correspondentAccount;
    }

    public void setCorrespondentAccount(String correspondentAccount) {
        this.correspondentAccount = correspondentAccount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getDirectorFirstname() {
        return directorFirstname;
    }

    public void setDirectorFirstname(String directorFirstname) {
        this.directorFirstname = directorFirstname;
    }

    public String getDirectorLastname() {
        return directorLastname;
    }

    public void setDirectorLastname(String directorLastname) {
        this.directorLastname = directorLastname;
    }

    public String getDirectorMiddlename() {
        return directorMiddlename;
    }

    public void setDirectorMiddlename(String directorMiddlename) {
        this.directorMiddlename = directorMiddlename;
    }

    @Override
    public String toString() {
        return "LegalEntity{" +
                "inn='" + inn + '\'' +
                ", kpp='" + kpp + '\'' +
                ", ogrn='" + ogrn + '\'' +
                ", address=" + address +
                ", bankName='" + bankName + '\'' +
                ", bik='" + bik + '\'' +
                ", correspondentAccount='" + correspondentAccount + '\'' +
                ", bankAccount='" + bankAccount + '\'' +
                ", directorFirstname='" + directorFirstname + '\'' +
                ", directorLastname='" + directorLastname + '\'' +
                ", directorMiddlename='" + directorMiddlename + '\'' +
                '}';
    }

        @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegalEntity that = (LegalEntity) o;
        return Objects.equals(inn, that.inn) &&
                Objects.equals(kpp, that.kpp) &&
                Objects.equals(ogrn, that.ogrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inn, kpp, ogrn);
    }
}
