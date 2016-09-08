package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "persons")
public class Person extends Resource {
    private List<String> phoneNumbers = new ArrayList<>();
    private List<String> emailAddresses = new ArrayList<>();
    private Resource passport;
    private Resource legalEntity;

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

    public List<String> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public Resource getPassport() {
        return passport;
    }

    public void setPassport(Resource passport) {
        this.passport = passport;
    }

    public Resource getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(Resource legalEntity) {
        this.legalEntity = legalEntity;
    }
}
