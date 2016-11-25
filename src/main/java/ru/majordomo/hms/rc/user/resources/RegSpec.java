package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegSpec {
    private List<String> states = new ArrayList<>();
    private DomainRegistrar registrar;
    private LocalDate created;
    private LocalDate paidTill;
    private LocalDate freeDate;

    public List<String> getStates() {
        return states;
    }

    public void setStates(List<String> states) {
        this.states = states;
    }

    public void addState(String state) {
        this.states.add(state);
    }

    public DomainRegistrar getRegistrar() {
        return registrar;
    }

    public void setRegistrar(DomainRegistrar registrar) {
        this.registrar = registrar;
    }

    public LocalDate getCreated() {
        return created;
    }

    @JsonGetter("created")
    public String getCreatedAsString() {
        return created.toString();
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    @JsonSetter("created")
    public void setCreatedAsString(String created) {
        this.created = LocalDate.parse(created);
    }

    public LocalDate getPaidTill() {
        return paidTill;
    }

    @JsonGetter("paidTill")
    public String getPaidTillAsString() {
        return paidTill.toString();
    }

    public void setPaidTill(LocalDate paidTill) {
        this.paidTill = paidTill;
    }

    @JsonSetter("paidTill")
    public void setPaidTillAsString(String paidTill) {
        this.paidTill = LocalDate.parse(paidTill);
    }

    public LocalDate getFreeDate() {
        return freeDate;
    }

    @JsonGetter("freeDate")
    public String getFreeDateAsString() {
        return freeDate.toString();
    }

    public void setFreeDate(LocalDate freeDate) {
        this.freeDate = freeDate;
    }

    @JsonSetter("freeDate")
    public void setFreeDateAsString(String freeDate) {
        this.freeDate = LocalDate.parse(freeDate);
    }
}
