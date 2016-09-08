package ru.majordomo.hms.rc.user.resources;

import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

public class RegSpec extends Resource {
    private List<String> states;
    private Resource person;
    private String registrar;
    private Long created;
    private Long paidTill;
    private Long freeDate;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public List<String> getStates() {
        return states;
    }

    public void setStates(List<String> states) {
        this.states = states;
    }

    public Resource getPerson() {
        return person;
    }

    public void setPerson(Resource person) {
        this.person = person;
    }

    public String getRegistrar() {
        return registrar;
    }

    public void setRegistrar(String registrar) {
        this.registrar = registrar;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getPaidTill() {
        return paidTill;
    }

    public void setPaidTill(Long paidTill) {
        this.paidTill = paidTill;
    }

    public Long getFreeDate() {
        return freeDate;
    }

    public void setFreeDate(Long freeDate) {
        this.freeDate = freeDate;
    }
}
