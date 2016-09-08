package ru.majordomo.hms.rc.user.resources;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

@Document(collection = "domains")
public class Domain extends Resource {

    private Resource regSpec;
    private List<Resource> dnsResourceRecords = new ArrayList<>();

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }

    public Resource getRegSpec() {
        return regSpec;
    }

    public void setRegSpec(Resource regSpec) {
        this.regSpec = regSpec;
    }

    public List<Resource> getDnsResourceRecords() {
        return dnsResourceRecords;
    }

    public void addDnsResourceRecord(Resource resourceRecord) {
        dnsResourceRecords.add(resourceRecord);
    }

    public void delDnsResourceRecord(Resource resourceRecord) {
        dnsResourceRecords.remove(resourceRecord);
    }

    @Override
    public String toString() {
        return "Domain{" +
                "id=" + this.getId() +
                ", name=" + this.getName() +
                ", regSpec=" + regSpec +
                ", dnsResourceRecords=" + dnsResourceRecords +
                '}';
    }
}
