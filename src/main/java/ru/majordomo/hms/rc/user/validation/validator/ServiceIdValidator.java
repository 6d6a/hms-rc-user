package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.validation.ServiceId;

@Component
public class ServiceIdValidator implements ConstraintValidator<ServiceId, String> {
    private StaffResourceControllerClient staffRcClient;

    @Autowired
    public ServiceIdValidator(
            StaffResourceControllerClient staffRcClient
    ) {
        this.staffRcClient = staffRcClient;
    }

    @Override
    public void initialize(ServiceId serviceId) {
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
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return isValid;
    }
}
