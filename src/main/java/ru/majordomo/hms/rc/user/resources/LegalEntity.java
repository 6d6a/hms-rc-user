package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "legalEntities")
public class LegalEntity extends Resource {
    private String inn;
    private String okpo;
    private String kpp;
    private String ogrn;
    private String okved;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
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

    public String getOkved() {
        return okved;
    }

    public void setOkved(String okved) {
        this.okved = okved;
    }
}
