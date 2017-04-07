package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.validation.ServiceId;

@Component
public class ServiceIdValidator implements ConstraintValidator<ServiceId, String> {
    private StaffResourceControllerClient staffRcClient;
    private String serviceName;
    private String defaultDatabaseServiceName;
    private String defaultWebSiteServiceName;

    @Autowired
    public ServiceIdValidator(
            StaffResourceControllerClient staffRcClient,
            @Value("${default.database.service.name}") String defaultDatabaseServiceName,
            @Value("${default.website.service.name}") String defaultWebSiteServiceName
    ) {
        this.staffRcClient = staffRcClient;
        this.defaultDatabaseServiceName = defaultDatabaseServiceName;
        this.defaultWebSiteServiceName = defaultWebSiteServiceName;
    }

    @Override
    public void initialize(ServiceId objectId) {
        switch (objectId.value()) {
            case WEBSITE:
                this.serviceName = defaultWebSiteServiceName;
                break;
            case DATABASE:
                this.serviceName = defaultDatabaseServiceName;
                break;
        }
    }

    @Override
    public boolean isValid(String serviceId, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = false;
        try {
            if (serviceId != null && !serviceId.equals("")) {
                Server server = staffRcClient.getServerByServiceId(serviceId);
                if (server != null) {
                    isValid = true;
                }
            } else {
                String serverId = staffRcClient.getActiveDatabaseServer().getId();

                List<Service> databaseServices = staffRcClient.getDatabaseServicesByServerIdAndServiceType(serverId);
                if (databaseServices != null) {
                    for (Service service : databaseServices) {
                        if (service.getServiceType().getName().equals(serviceName)) {
                            isValid = true;
                            break;
                        }
                    }
                    if (!isValid) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate("Не найдено serviceType: " + serviceName +
                                        " для сервера: " + serverId)
                                .addConstraintViolation();
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return isValid;
    }
}
