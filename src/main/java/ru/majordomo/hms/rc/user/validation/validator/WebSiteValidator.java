package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.WebSite;
import ru.majordomo.hms.rc.user.validation.ValidWebSite;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class WebSiteValidator implements ConstraintValidator<ValidWebSite, WebSite> {
    private final MongoOperations operations;
    private StaffResourceControllerClient staffRcClient;
    private String serviceName;

    @Autowired
    public WebSiteValidator(
            MongoOperations operations,
            StaffResourceControllerClient staffRcClient,
            @Value("${default.website.serviceName}") String defaultWebSiteServiceName
    ) {
        this.operations = operations;
        this.staffRcClient = staffRcClient;
        serviceName = defaultWebSiteServiceName;
    }

    @Override
    public void initialize(ValidWebSite validWebSite) {
    }

    @Override
    public boolean isValid(final WebSite webSite, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;

            if (webSite.getId() != null) {
                query = new Query(where("id").nin(webSite.getId()).and("domainIds").in(webSite.getDomainIds()));
            } else {
                query = new Query(where("domainIds").in(webSite.getDomainIds()));
            }

            isValid = !operations.exists(query, Mailbox.class);
        } catch (RuntimeException e) {
            return false;
        }

//        try {
//            String serviceId = webSite.getServiceId();
//
//            if (serviceId != null && !serviceId.equals("")) {
//                String serverId = webSite.getUnixAccount().getServerId();
//                List<Service> services = staffRcClient.getWebsiteServicesByServerId(serverId);
//
//                for (Service service : services) {
//                    if (service.getId().equals(serviceId)) {
//                        isValid = true;
//                        break;
//                    }
//                }
//            }
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//        }

        return isValid;
    }
}
