package ru.majordomo.hms.rc.user.resources;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;

public class LegalEntity {
    @NotBlank(groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Length.List(
            {
                    @Length(min = 10, max = 10, groups = {PersonCompanyChecks.class}),
                    @Length(min = 12, max = 12, groups = {PersonEntrepreneurChecks.class})
            }
    )
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    private String inn;

    private String okpo;

    @NotBlank(groups = {PersonCompanyChecks.class})
    @Length(min = 9, max = 9, groups = {PersonCompanyChecks.class})
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class})
    private String kpp;

    @NotBlank(groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Length.List(
            {
                    @Length(min = 13, max = 13, groups = {PersonCompanyChecks.class}),
                    @Length(min = 15, max = 15, groups = {PersonEntrepreneurChecks.class})
            }
    )
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    private String ogrn;

    private String okvedCodes;

    @Valid
    private Address address;

    @Length(max = 128, groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(
            regexp = "(?ui)(^([а-яё0-9\\,\\.\"\\(\\)№ -]+)$|^([a-z0-9\\,\\.\"\\(\\)# -]+)$)",
            groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class}
    )
    private String bankName;

    @Length(min = 9, max = 9, groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    private String bik;

    @Length(min = 20, max = 20, groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    private String correspondentAccount;

    @Length(min = 20, max = 20, groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    @Pattern(regexp = "(^[\\d]+$)", groups = {PersonCompanyChecks.class, PersonEntrepreneurChecks.class})
    private String bankAccount;

    @NotNull(groups = {PersonCompanyChecks.class})
    @Length(min = 2, max = 64, groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class})
    @Pattern.List(
            {
                    @Pattern(regexp = "(?ui)(^[а-яё]+$)", groups = {PersonCompanyChecks.class}),
                    @Pattern(regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$)", groups = {PersonCompanyForeignChecks.class})
            }
    )
    private String directorFirstname;

    @NotNull(groups = {PersonCompanyChecks.class})
    @Length(min = 2, max = 64, groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class})
    @Pattern.List(
            {
                    @Pattern(regexp = "(?ui)(^[а-яё-]+$)", groups = {PersonCompanyChecks.class}),
                    @Pattern(regexp = "(?ui)(^([а-яё-]+)$|^([a-z-]+)$)", groups = {PersonCompanyForeignChecks.class})
            }
    )
    private String directorLastname;

    @Length(max = 64, groups = {PersonCompanyChecks.class, PersonCompanyForeignChecks.class})
    @Pattern.List(
            {
                    @Pattern(regexp = "(?ui)(^[а-яё]+$)", groups = {PersonCompanyChecks.class}),
                    @Pattern(regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$)", groups = {PersonCompanyForeignChecks.class})
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

    public String getOkpo() {
        return okpo;
    }

    public void setOkpo(String okpo) {
        this.okpo = okpo;
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

    public String getOkvedCodes() {
        return okvedCodes;
    }

    public void setOkvedCodes(String okvedCodes) {
        this.okvedCodes = okvedCodes;
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
                ", okpo='" + okpo + '\'' +
                ", kpp='" + kpp + '\'' +
                ", ogrn='" + ogrn + '\'' +
                ", okvedCodes='" + okvedCodes + '\'' +
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
                Objects.equals(okpo, that.okpo) &&
                Objects.equals(kpp, that.kpp) &&
                Objects.equals(ogrn, that.ogrn) &&
                Objects.equals(okvedCodes, that.okvedCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inn, okpo, kpp, ogrn, okvedCodes);
    }
}
