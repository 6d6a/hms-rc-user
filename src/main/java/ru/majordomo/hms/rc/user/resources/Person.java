package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "persons")
public class Person extends Resource {
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> emailAddresses = new ArrayList<>();
    private Passport passport;
    private LegalEntity legalEntity;
    private String country;
    private String postalAddress;
    private Boolean owner;

    public Boolean getOwner() {
        return owner;
    }

    public void setOwner(Boolean owner) {
        this.owner = owner;
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(phoneNumbers, person.phoneNumbers) &&
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
                super.toString() +
                ", phoneNumbers=" + phoneNumbers +
                ", emailAddresses=" + emailAddresses +
                ", passport=" + passport +
                ", legalEntity=" + legalEntity +
                ", country='" + country + '\'' +
                ", postalAddress='" + postalAddress + '\'' +
                ", owner=" + owner +
                '}';
    }
}
