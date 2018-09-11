package ru.majordomo.hms.rc.user.service.stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.api.DTO.stat.BaseResourceIdCount;
import ru.majordomo.hms.rc.user.api.DTO.stat.QuotableResourceCount;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Service
public class Aggregator {

    private MongoOperations mongoOperations;
    private StaffResourceControllerClient rcStaffClient;

    @Autowired
    public Aggregator(
            MongoOperations mongoOperations,
            StaffResourceControllerClient rcStaffClient
    ) {
        this.mongoOperations = mongoOperations;
        this.rcStaffClient = rcStaffClient;
    }

    public <T extends Resource> Collection getStat(Class<T> tClass) {
        GroupOperation group = group();
        Class<? extends BaseResourceIdCount> resultClass = BaseResourceIdCount.class;

        if (Serviceable.class.isAssignableFrom(tClass)) {
            group = group("serviceId", "switchedOn")
                    .first("serviceId").as("resourceId");
        } else if (ServerStorable.class.isAssignableFrom(tClass)) {
            group = group("serverId", "switchedOn")
                    .first("serverId").as("resourceId");
        }

        if (Quotable.class.isAssignableFrom(tClass)) {
            group = group.sum("quotaUsed").as("quotaUsed");
            resultClass = QuotableResourceCount.class;
        }

        group = group.first("switchedOn").as("active")
                .count().as("count");

        Aggregation aggregation = Aggregation.newAggregation(group);

        List<? extends BaseResourceIdCount> result = mongoOperations.aggregate(
                aggregation, tClass, resultClass
        ).getMappedResults();

        Map<String, String> idAndName = new HashMap<>();

        if (Serviceable.class.isAssignableFrom(tClass)) {
            idAndName = rcStaffClient
                    .getServicesOnlyIdAndName().stream().collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));

        } else if (ServerStorable.class.isAssignableFrom(tClass)) {
            idAndName = rcStaffClient
                    .getServersOnlyIdAndName().stream().collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));
        }

        for (BaseResourceIdCount c : result) {
            c.setName(idAndName.get(c.getResourceId()));
        }

        return result;
    }

    public List<AccountIdAndField> getAccountIdAndField(String resource, String fieldName) {
        Class tClass;
        switch (resource) {
            case "unix-account":
                tClass = UnixAccount.class;
                break;
            case "mailbox":
                tClass = Mailbox.class;
                break;
            default:
                throw new ParameterValidationException("Неизвестный тип ресурса");
        }

        Aggregation aggregation = Aggregation.newAggregation(
                project("accountId").and(fieldName).as("field")
        );

        return mongoOperations
                .aggregate(aggregation, tClass, AccountIdAndField.class)
                .getMappedResults();
    }

    public class AccountIdAndField {
        private String accountId;
        private String field;
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
    }
}
