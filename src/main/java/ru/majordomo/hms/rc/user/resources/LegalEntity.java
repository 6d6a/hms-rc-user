package ru.majordomo.hms.rc.user.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LegalEntity {
    private String inn;
    private String okpo;
    private String kpp;
    private String ogrn;
    private List<String> okvedCodes = new ArrayList<>();

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

    public List<String> getOkvedCodes() {
        return okvedCodes;
    }

    public void setOkvedCodes(List<String> okvedCodes) {
        this.okvedCodes = okvedCodes;
    }

    public void addOkved(String okved) {
        this.okvedCodes.add(okved);
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
