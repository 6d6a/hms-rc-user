package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.validation.group.DatabaseUserChecks;

@Component
public class GovernorOfDatabaseUser extends LordOfResources<DatabaseUser> {
    private Cleaner cleaner;
    private DatabaseUserRepository repository;
    private GovernorOfDatabase governorOfDatabase;
    private Validator validator;

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
    public DatabaseUser create(ServiceMessage serviceMessage) throws ParameterValidateException {
        DatabaseUser databaseUser;
        try {
            databaseUser = buildResourceFromServiceMessage(serviceMessage);
            validate(databaseUser);
            store(databaseUser);

            if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
                for (String databaseId : databaseUser.getDatabaseIds()) {
                    Database database = governorOfDatabase.build(databaseId);
                    database.addDatabaseUserId(databaseUser.getId());
                    governorOfDatabase.validate(database);
                    governorOfDatabase.store(database);
                }
            }

        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidateException("В пароле используются некорретные символы");
        }
        return databaseUser;
    }

    @Override
    public DatabaseUser update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
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
                            throw new ParameterValidateException("Неверный формат IP-адреса");
                        }
                        break;
                    case "switchedOn":
                        databaseUser.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

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
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected DatabaseUser buildResourceFromServiceMessage(ServiceMessage serviceMessage)
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
                    throw new ParameterValidateException("Недопустимый тип баз данных");
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
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        databaseUser.setDatabaseIds(databaseIds);
        databaseUser.setServiceId(serviceId);
        databaseUser.setType(userType);
        try {
            databaseUser.setPasswordHashByPlainPassword(password);
        } catch (IllegalArgumentException e) {
            throw new ParameterValidateException(e.getMessage());
        }
        try {
            databaseUser.setAllowedIpsAsCollectionOfString(allowedIps);
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("Неверный формат IP-адреса");
        }


        return databaseUser;
    }

    @Override
    public void validate(DatabaseUser databaseUser) throws ParameterValidateException {
        Set<ConstraintViolation<DatabaseUser>> constraintViolations = validator.validate(databaseUser, DatabaseUserChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug(constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected DatabaseUser construct(DatabaseUser databaseUser) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseUser build(String resourceId) throws ResourceNotFoundException {
        DatabaseUser resource = repository.findOne(resourceId);
        if (resource != null) {
            return resource;
        } else {
            throw new ResourceNotFoundException("Пользователь баз данных с ID: " + resourceId + " не найден");
        }
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

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            buildedDatabasesUsers = repository.findByAccountId(keyValue.get("accountId"));
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
