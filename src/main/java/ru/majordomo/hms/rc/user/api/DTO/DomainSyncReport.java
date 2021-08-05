package ru.majordomo.hms.rc.user.api.DTO;

import lombok.Data;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.RegSpec;

import java.util.List;
import java.util.Map;

@Data
public class DomainSyncReport {
    private Map<String, RegSpec> params;
    private List<DomainRegistrar> problemRegistrars;

    public DomainSyncReport(){}

    public DomainSyncReport(Map<String, RegSpec> params, List<DomainRegistrar> problemRegistrars) {
        this.params = params;
        this.problemRegistrars = problemRegistrars;
    }
}
