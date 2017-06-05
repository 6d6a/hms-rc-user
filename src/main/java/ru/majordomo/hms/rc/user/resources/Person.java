package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;
import ru.majordomo.hms.rc.user.resources.validation.ValidPerson;
import ru.majordomo.hms.rc.user.resources.validation.ValidPhone;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonChecks;
import ru.majordomo.hms.rc.user.resources.validation.groupSequenceProvider.PersonGroupSequenceProvider;

@Document(collection = "persons")
@ValidPerson
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
    private String country;

    @Valid
    private Address postalAddress;
    private String nicHandle;
    private List<String> linkedAccountIds = new ArrayList<>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(phoneNumbers, person.phoneNumbers) &&
                Objects.equals(type, person.type) &&
                Objects.equals(nicHandle, person.nicHandle) &&
                Objects.equals(emailAddresses, person.emailAddresses) &&
                Objects.equals(passport, person.passport) &&
                Objects.equals(legalEntity, person.legalEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumbers, emailAddresses, passport, legalEntity);
    }

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
                "} " + super.toString();
    }
}
