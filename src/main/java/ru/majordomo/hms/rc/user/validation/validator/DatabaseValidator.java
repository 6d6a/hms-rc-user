package ru.majordomo.hms.rc.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.validation.ValidDatabase;

public class DatabaseValidator implements ConstraintValidator<ValidDatabase, Database> {
    private StaffResourceControllerClient staffRcClient;

    @Autowired
    public DatabaseValidator(
            StaffResourceControllerClient staffRcClient
    ) {
        this.staffRcClient = staffRcClient;
    }

    @Override
    public void initialize(ValidDatabase validBillingOperation) {
    }

    @Override
    public boolean isValid(final Database database, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;

        try {
            DBType dbType = database.getType();
            if (database.getDatabaseUsers() != null) {
                for (DatabaseUser databaseUser : database.getDatabaseUsers()) {
                    DBType userType = databaseUser.getType();
                    if (dbType != userType) {
                        isValid = false;
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate("Тип базы данных: " + dbType +
                                        ". Тип пользователя с ID " + databaseUser.getId() + ": " + userType +
                                        ". Типы должны совпадать")
                                .addConstraintViolation();
                    }
                }
            }
        } catch (RuntimeException e) {
            System.out.println(e.toString());
            isValid = false;
        }

//        try {
//            String serviceId = database.getServiceId();
//
//            if (serviceId != null && !serviceId.equals("")) {
//                String serverId = staffRcClient.getActiveDatabaseServer().getId();
//                List<Service> services = staffRcClient.getDatabaseServicesByServerId(serverId);
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
