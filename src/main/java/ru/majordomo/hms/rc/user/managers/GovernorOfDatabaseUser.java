package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.Utils;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserImportChecks;

@Component
public class GovernorOfDatabaseUser extends LordOfResources<DatabaseUser> {
    private Cleaner cleaner;
    private DatabaseUserRepository repository;
    private GovernorOfDatabase governorOfDatabase;
    private Validator validator;
    private String defaultServiceName;
    private StaffResourceControllerClient staffRcClient;

    @Value("${default.database.serviceName}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setRepository(DatabaseUserRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setGovernorOfDatabase(GovernorOfDatabase governorOfDatabase) {
        this.governorOfDatabase = governorOfDatabase;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public DatabaseUser create(ServiceMessage serviceMessage) throws ParameterValidationException {
        DatabaseUser databaseUser;
        try {
            databaseUser = buildResourceFromServiceMessage(serviceMessage);
            preValidate(databaseUser);
            validate(databaseUser);
            store(databaseUser);

            if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
                for (String databaseId : databaseUser.getDatabaseIds()) {
                    Database database = governorOfDatabase.build(databaseId);
                    database.addDatabaseUserId(databaseUser.getId());
                    governorOfDatabase.preValidate(database);
                    governorOfDatabase.validate(database);
                    governorOfDatabase.store(database);
                }
            }

        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidationException("В пароле используются некорретные символы");
        }
        return databaseUser;
    }

    @Override
    public DatabaseUser update(ServiceMessage serviceMessage)
            throws ParameterValidationException, UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        DatabaseUser databaseUser = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        databaseUser.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "allowedAddressList":
                        try {
                            databaseUser.setAllowedIpsAsCollectionOfString(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        } catch (NumberFormatException e) {
                            throw new ParameterValidationException("Неверный формат IP-адреса");
                        }
                        break;
                    case "switchedOn":
                        databaseUser.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "serviceId":
                        databaseUser.setServiceId((String) entry.getValue());
                        break;
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            databaseUser.setWillBeDeletedAfter(null);
                        } else {
                            databaseUser.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    case "maxCpuTimePerSecond":
                        BigDecimal maxCpuTimePerSecond = Utils.getBigDecimalFromUnexpectedInput(
                                serviceMessage.getParam("maxCpuTimePerSecond")
                        );
                        databaseUser.setMaxCpuTimePerSecond(maxCpuTimePerSecond);

                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(databaseUser);
        validate(databaseUser);
        store(databaseUser);

        return databaseUser;
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfDatabase.removeDatabaseUserIdFromDatabases(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    @Override
    public DatabaseUser buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException, UnsupportedEncodingException {
        DatabaseUser databaseUser = new DatabaseUser();
        setResourceParams(databaseUser, serviceMessage, cleaner);
        String password = null;
        DBType userType = null;
        String serviceId = null;
        String userTypeAsString;
        List<String> allowedIps = null;
        List<String> databaseIds = null;

        try {
            if (serviceMessage.getParam("password") != null) {
                password = cleaner.cleanString((String) serviceMessage.getParam("password"));
            }

            if (serviceMessage.getParam("type") != null) {
                userTypeAsString = cleaner.cleanString((String) serviceMessage.getParam("type"));
                try {
                    userType = Enum.valueOf(DBType.class, userTypeAsString);
                } catch (IllegalArgumentException e) {
                    throw new ParameterValidationException("Недопустимый тип баз данных");
                }
            }

            if (serviceMessage.getParam("serviceId") != null) {
                serviceId = cleaner.cleanString((String) serviceMessage.getParam("serviceId"));
            }

            if (serviceMessage.getParam("databaseIds") != null) {
                databaseIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("databaseIds"));
            }

            if (serviceMessage.getParam("allowedAddressList") != null) {
                allowedIps = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("allowedAddressList"));
            }

            if (serviceMessage.getParams().containsKey("maxCpuTimePerSecond")) {
                BigDecimal maxCpuTimePerSecond = null;
                try {
                    maxCpuTimePerSecond = Utils.getBigDecimalFromUnexpectedInput(serviceMessage.getParam("maxCpuTimePerSecond"));
                } catch (ParameterValidationException ignore) {} //this means that there is no limit
                databaseUser.setMaxCpuTimePerSecond(maxCpuTimePerSecond);
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        databaseUser.setDatabaseIds(databaseIds);
        databaseUser.setServiceId(serviceId);
        databaseUser.setType(userType);
        try {
            databaseUser.setPasswordHashByPlainPassword(password);
        } catch (IllegalArgumentException e) {
            throw new ParameterValidationException(e.getMessage());
        }
        try {
            databaseUser.setAllowedIpsAsCollectionOfString(allowedIps);
        } catch (NumberFormatException e) {
            throw new ParameterValidationException("Неверный формат IP-адреса");
        }


        return databaseUser;
    }

    @Override
    public void preValidate(DatabaseUser databaseUser) {
        preValidateDatabaseServiceId(databaseUser, staffRcClient, defaultServiceName);
    }

    @Override
    public void validate(DatabaseUser databaseUser) throws ParameterValidationException {
        Set<ConstraintViolation<DatabaseUser>> constraintViolations = validator.validate(databaseUser, DatabaseUserChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("databaseUser: " + databaseUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(DatabaseUser databaseUser) {
        Set<ConstraintViolation<DatabaseUser>> constraintViolations = validator.validate(databaseUser, DatabaseUserImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] databaseUser: " + databaseUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected DatabaseUser construct(DatabaseUser databaseUser) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseUser build(String resourceId) throws ResourceNotFoundException {
        DatabaseUser resource = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь баз данных с ID: " + resourceId + " не найден"));

        return resource;
    }

    @Override
    public DatabaseUser build(Map<String, String> keyValue) throws ResourceNotFoundException {
        DatabaseUser databaseUser = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            databaseUser = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (databaseUser == null) {
            throw new ResourceNotFoundException("Пользователь баз данных с ID:" + keyValue.get("resourceId") +
                    " и account ID:" + keyValue.get("accountId") + " не найден");
        }

        return databaseUser;
    }

    @Override
    public Collection<DatabaseUser> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DatabaseUser> buildedDatabasesUsers = new ArrayList<>();

        if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            buildedDatabasesUsers.addAll(repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId")));
        } else if (keyValue.get("accountId") != null) {
            buildedDatabasesUsers = repository.findByAccountId(keyValue.get("accountId"));
        } else if (keyValue.get("serviceId") != null) {
            buildedDatabasesUsers.addAll(repository.findByServiceId(keyValue.get("serviceId")));
        }

        return buildedDatabasesUsers;
    }

    @Override
    public Collection<DatabaseUser> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(DatabaseUser databaseUser) {
        repository.save(databaseUser);
    }
}
