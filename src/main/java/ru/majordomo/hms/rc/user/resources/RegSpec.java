package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegSpec {
    private List<DomainState> states = new ArrayList<>();
    private DomainRegistrar registrar;
    private LocalDate created;
    private LocalDate paidTill;
    private LocalDate freeDate;

    public List<DomainState> getStates() {
        return states;
    }

    public void setStates(List<DomainState> states) {
        this.states = states;
    }

    public void addState(DomainState state) {
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
        if (created != null)
            return created.toString();
        return null;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    @JsonSetter("created")
    public void setCreatedAsString(String created) {
        if (created != null) {
            this.created = LocalDate.parse(created);
        }
    }

    public LocalDate getPaidTill() {
        return paidTill;
    }

    @JsonGetter("paidTill")
    public String getPaidTillAsString() {
        if (paidTill != null)
            return paidTill.toString();
        return null;
    }

    public void setPaidTill(LocalDate paidTill) {
        this.paidTill = paidTill;
    }

    @JsonSetter("paidTill")
    public void setPaidTillAsString(String paidTill) {
        if (paidTill != null) {
            this.paidTill = LocalDate.parse(paidTill);
        }
    }

    public LocalDate getFreeDate() {
        return freeDate;
    }

    @JsonGetter("freeDate")
    public String getFreeDateAsString() {
        if (freeDate != null)
            return freeDate.toString();
        return null;
    }

    public void setFreeDate(LocalDate freeDate) {
        this.freeDate = freeDate;
    }

    @JsonSetter("freeDate")
    public void setFreeDateAsString(String freeDate) {
        if (freeDate != null) {
            this.freeDate = LocalDate.parse(freeDate);
        }
    }

    @Override
    public String toString() {
        return " RegSpec: " +
                "{ states=" + states +
                ", registrar=" + registrar +
                ", created=" + created +
                ", paidTill=" + paidTill +
                ", freeDate=" + freeDate +
                " }";
    }
}
