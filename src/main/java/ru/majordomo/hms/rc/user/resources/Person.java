package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mysql.management.util.Str;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;
import ru.majordomo.hms.rc.user.resources.validation.ValidPhone;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.groupSequenceProvider.PersonGroupSequenceProvider;

import static ru.majordomo.hms.rc.user.resources.Constants.COUNTRY_FOREIGN_PATTERN;

@Document(collection = "persons")
@GroupSequenceProvider(value = PersonGroupSequenceProvider.class)
public class Person extends Resource {
    @NotNull(message = "Должен быть указан тип персоны")
    private PersonType type;

    @Valid
    private List<@ValidPhone(groups = PersonChecks.class) String> phoneNumbers = new ArrayList<>();

    @NotEmpty(message = "Должен быть указан хотя бы 1 email адрес")
    @Valid
    private List<@ValidEmail(groups = PersonChecks.class) String> emailAddresses = new ArrayList<>();

    @Valid
    private Passport passport;

    @Valid
    private LegalEntity legalEntity;

    @NotBlank(message = "Страна должна быть указана")
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(^RU$)",
                            groups = {PersonIndividualChecks.class},
                            message = "Для граждан РФ страна должна быть указана как 'RU'"
                    ),
                    @Pattern(
                            regexp = COUNTRY_FOREIGN_PATTERN,
                            groups = {PersonIndividualForeignChecks.class},
                            message = "Неверно указана страна"
                    )
            }
    )
    private String country;

    @Valid
    private Address postalAddress;

    private String nicHandle;
    private List<String> linkedAccountIds = new ArrayList<>();

    @NotBlank(
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Имя' обязательно для заполнения"
    )
    @Length(
            min = 2,
            max = 64,
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Имя' должно содержать от {min} до {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё]+$)",
                            groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class},
                            message = "В поле 'Имя' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$)",
                            groups = {PersonIndividualForeignChecks.class, PersonEntrepreneurForeignChecks.class},
                            message = "В поле 'Имя' разрешены символы только русского или только латинского алфавита"
                    )
            }
    )
    private String firstname;

    @NotBlank(
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Фамилия' обязательно для заполнения"
    )
    @Length(
            min = 2,
            max = 64,
            groups = {
                    PersonIndividualChecks.class,
                    PersonIndividualForeignChecks.class,
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Фамилия' должно содержать от {min} до {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё-]+$)",
                            groups = {PersonIndividualChecks.class, PersonEntrepreneurChecks.class},
                            message = "В поле 'Фамилия' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё-]+)$|^([a-z-]+)$)",
                            groups = {PersonIndividualForeignChecks.class, PersonEntrepreneurForeignChecks.class},
                            message = "В поле 'Фамилия' разрешены символы только русского или только латинского алфавита и дефис"
                    )
            }
    )
    private String lastname;

    @NotBlank(
            groups = {
                    PersonEntrepreneurChecks.class,
                    PersonEntrepreneurForeignChecks.class
            },
            message = "Поле 'Отчество' обязательно для заполнения"
    )
    @Length.List(
            {
                    @Length(
                            max = 64,
                            groups = {
                                    PersonIndividualChecks.class,
                                    PersonIndividualForeignChecks.class
                            },
                            message = "Поле 'Отчество' должно содержать максимум {max} символа"
                    ),
                    @Length(
                            min = 2,
                            max = 64,
                            groups = {
                                    PersonEntrepreneurChecks.class,
                                    PersonEntrepreneurForeignChecks.class
                            },
                            message = "Поле 'Отчество' должно содержать от {min} до {max} символов"
                    )
            }
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[а-яё]+$|^$)",
                            groups = {PersonIndividualChecks.class},
                            message = "В поле 'Отчество' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^[а-яё]+$)",
                            groups = {PersonEntrepreneurChecks.class},
                            message = "В поле 'Отчество' разрешены только символы русского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$)",
                            groups = {PersonEntrepreneurForeignChecks.class},
                            message = "В поле 'Отчество' разрешены символы только русского или только латинского алфавита"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё]+)$|^([a-z]+)$|^$)",
                            groups = {PersonIndividualForeignChecks.class},
                            message = "В поле 'Отчество' разрешены символы только русского или только латинского алфавита"
                    )
            }
    )
    private String middlename;

    @Length(
            max = 64,
            groups = {PersonCompanyChecks.class},
            message = "Поле 'Организационно-правовая форма компании' должно содержать максимум {max} символа"
    )
    @Pattern(
            regexp = "(?ui)(^[а-яё -]+$|^$)",
            groups = {PersonCompanyChecks.class},
            message = "В поле 'Организационно-правовая форма компании' разрешены только символы русского алфавита"
    )
    private String orgForm;

    @NotBlank(
            groups = {PersonCompanyChecks.class},
            message = "Поле 'Название компании' обязательно для заполнения"
    )
    @Length(
            min = 1,
            max = 255,
            groups = {PersonCompanyChecks.class},
            message = "Поле 'Название компании' должно содержать от {min} до {max} символов"
    )
    @Pattern.List(
            {
                    @Pattern(
                            regexp = "(?ui)(^[a-zа-яё0-9№\\/\\,\\.\\+ !-]+$)",
                            groups = {PersonCompanyChecks.class},
                            message = "В поле 'Название компании' разрешены символы русского и латинского алфавита, а также '№', '/', ',', '.', '+', '!' и '-'"
                    ),
                    @Pattern(
                            regexp = "(?ui)(^([а-яё0-9'\\/\\,\\.\\+ -]+)$|^([a-z0-9\\/\\,\\.\\+ -]+)$)",
                            groups = {PersonCompanyForeignChecks.class},
                            message = "В поле 'Название компании' разрешены символы только русского или только латинского алфавита, а также '/', ',', '.', '+' и '-'"
                    )
            }
    )
    private String orgName;

    public PersonType getType() {
        return type;
    }

    public void setType(PersonType type) {
        this.type = type;
    }

    public String getNicHandle() {
        return nicHandle;
    }

    public void setNicHandle(String nicHandle) {
        this.nicHandle = nicHandle;
    }

    @JsonGetter(value = "postalAddress")
    public Address getPostalAddress() {
        return postalAddress;
    }

    @JsonSetter(value = "postalAddress")
    public void setPostalAddress(Address address) {
        this.postalAddress = address;
    }

    @JsonIgnore
    public String getPostalAddressAsString() {
        return this.postalAddress == null ? null : this.postalAddress.toString();
    }

    @JsonIgnore
    public void setPostalAddress(String postalAddress) {
        this.postalAddress = new Address(postalAddress);
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public void addPhoneNumber(String phoneNumber) {
        this.phoneNumbers.add(phoneNumber);
    }

    public void delPhoneNumber(String phoneNumber) {
        this.phoneNumbers.remove(phoneNumber);
    }

    public List<String> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public void addEmailAddress(String emailAddress) {
        this.emailAddresses.add(emailAddress);
    }

    public void delEmailAddress(String emailAddress) {
        this.emailAddresses.remove(emailAddress);
    }

    public Passport getPassport() {
        return passport;
    }

    public void setPassport(Passport passport) {
        this.passport = passport;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(LegalEntity legalEntity) {
        this.legalEntity = legalEntity;
    }

    public List<String> getLinkedAccountIds() {
        return linkedAccountIds;
    }

    public void setLinkedAccountIds(List<String> linkedAccountIds) {
        this.linkedAccountIds = linkedAccountIds;
    }

    public void addLinkedAccountId(String linkedAccountId) {
        if (!linkedAccountIds.contains(linkedAccountId)) {
            linkedAccountIds.add(linkedAccountId);
        }
    }

    public void removeLinkedAccountId(String linkedAccountId) {
        if (linkedAccountIds.contains(linkedAccountId)) {
            linkedAccountIds.remove(linkedAccountId);
        }
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getOrgForm() {
        return orgForm;
    }

    public void setOrgForm(String orgForm) {
        this.orgForm = orgForm;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Person person = (Person) o;
//        return Objects.equals(phoneNumbers, person.phoneNumbers) &&
//                Objects.equals(type, person.type) &&
//                Objects.equals(nicHandle, person.nicHandle) &&
//                Objects.equals(emailAddresses, person.emailAddresses) &&
//                Objects.equals(passport, person.passport) &&
//                Objects.equals(legalEntity, person.legalEntity) &&
//                Objects.equals(firstname, person.firstname) &&
//                Objects.equals(lastname, person.lastname) &&
//                Objects.equals(middlename, person.middlename) &&
//                Objects.equals(orgForm, person.orgForm) &&
//                Objects.equals(orgName, person.orgName);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(phoneNumbers, emailAddresses, passport, legalEntity);
//    }

    @Override
    public String toString() {
        return "Person{" +
                "type=" + type +
                ", phoneNumbers=" + phoneNumbers +
                ", emailAddresses=" + emailAddresses +
                ", passport=" + passport +
                ", legalEntity=" + legalEntity +
                ", country='" + country + '\'' +
                ", postalAddress=" + postalAddress +
                ", nicHandle='" + nicHandle + '\'' +
                ", linkedAccountIds=" + linkedAccountIds +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", middlename='" + middlename + '\'' +
                ", orgForm='" + orgForm + '\'' +
                ", orgName='" + orgName + '\'' +
                "} " + super.toString();
    }

    @Override
    public String getName() {
        String name = "";

        if(type != null){
            switch (type) {
                case ENTREPRENEUR:
                case ENTREPRENEUR_FOREIGN:
                    name += "ИП ";
                case INDIVIDUAL:
                case INDIVIDUAL_FOREIGN:
                    if (lastname != null && !lastname.equals("")) {
                        name += lastname + " ";
                    }

                    if (firstname != null && !firstname.equals("")) {
                        name += firstname + " ";
                    }

                    if (middlename != null && !middlename.equals("")) {
                        name += middlename;
                    }
                    break;
                case COMPANY:
                case COMPANY_FOREIGN:
                    if (orgForm != null && !orgForm.equals("")) {
                        name += orgForm + " ";
                    }

                    if (orgName != null && !orgName.equals("")) {
                        name += orgName;
                    }
                    break;
            }
        }

        if (name.equals("")) {
            name = super.getName();
        }

        return name;
    }
}
