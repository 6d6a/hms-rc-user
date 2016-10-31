package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "persons")
public class Person extends Resource {
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> emailAddresses = new ArrayList<>();
    private Passport passport;
    private LegalEntity legalEntity;

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
    public String toString() {
        return "Person{" +
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", switchedOn" + this.getSwitchedOn() +
                ", phoneNumbers=" + phoneNumbers +
                ", emailAddresses=" + emailAddresses +
                ", passport=" + passport +
                ", legalEntity=" + legalEntity +
                '}';
    }
}
