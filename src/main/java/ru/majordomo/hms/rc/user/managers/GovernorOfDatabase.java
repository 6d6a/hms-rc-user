package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.validation.group.DatabaseChecks;

@Component
public class GovernorOfDatabase extends LordOfResources<Database> {

    private DatabaseRepository repository;
    private Cleaner cleaner;
    private GovernorOfDatabaseUser governorOfDatabaseUser;
    private GovernorOfResourceArchive governorOfResourceArchive;
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
    public void setGovernorOfDatabaseUser(GovernorOfDatabaseUser governorOfDatabaseUser) {
        this.governorOfDatabaseUser = governorOfDatabaseUser;
    }

    @Autowired
    public void setGovernorOfResourceArchive(GovernorOfResourceArchive governorOfResourceArchive) {
        this.governorOfResourceArchive = governorOfResourceArchive;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setRepository(DatabaseRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Database update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Database database = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "databaseUserIds":
                        database.setDatabaseUserIds((List<String>) entry.getValue());
                        break;
                    case "switchedOn":
                        database.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        preValidate(database);
        validate(database);
        store(database);

        return database;
    }

    public void removeDatabaseUserIdFromDatabases(String databaseUserId) {
        List<Database> databases = repository.findByDatabaseUserIdsContaining(databaseUserId);
        for (Database database : databases) {
            List<String> databaseUserIds = database.getDatabaseUserIds();
            databaseUserIds.remove(databaseUserId);
            database.setDatabaseUserIds(databaseUserIds);
        }
        repository.save(databases);
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfResourceArchive.dropByResourceId(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Database c ID: " + resourceId + " не найден");
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected Database buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Database database = new Database();
        String serviceId = null;
        DBType type = null;

        setResourceParams(database, serviceMessage, cleaner);

        if (serviceMessage.getParam("serviceId") != null) {
            serviceId = cleaner.cleanString((String) serviceMessage.getParam("serviceId"));
        }

        if (serviceMessage.getParam("type") != null) {
            for (DBType dbType : DBType.values()) {
                if (dbType.name().equals(serviceMessage.getParam("type").toString())) {
                    type = DBType.valueOf(serviceMessage.getParam("type").toString());
                    break;
                }
            }
        }

        List<String> databaseUserIds;
        if (serviceMessage.getParam("databaseUserIds") != null) {
            databaseUserIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("databaseUserIds"));

            database.setDatabaseUserIds(databaseUserIds);
            construct(database);
        }

        database.setServiceId(serviceId);
        database.setQuota(0L);
        database.setQuotaUsed(0L);
        database.setWritable(true);
        database.setType(type);

        return database;
    }

    @Override
    public void preValidate(Database database) {
        preValidateDatabaseServiceId(database, staffRcClient, defaultServiceName);
    }

    @Override
    public void validate(Database database) throws ParameterValidateException {
        Set<ConstraintViolation<Database>> constraintViolations = validator.validate(database, DatabaseChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("database: " + database + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected Database construct(Database database) {
        List<DatabaseUser> databaseUsers = new ArrayList<>();
        for (String databaseUserId : database.getDatabaseUserIds()) {
            databaseUsers.add(governorOfDatabaseUser.build(databaseUserId));
        }
        database.setDatabaseUsers(databaseUsers);
        return database;
    }

    @Override
    public Database build(String resourceId) throws ResourceNotFoundException {
        Database database = repository.findOne(resourceId);
        if (database == null) {
            throw new ResourceNotFoundException("Database с ID:" + resourceId + " не найдена");
        }
        return construct(database);
    }

    @Override
    public Database build(Map<String, String> keyValue) throws ResourceNotFoundException {

        Database database = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            database = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (database == null) {
            throw new ResourceNotFoundException("База данных не найдена");
        }

        return construct(database);
    }

    @Override
    public Collection<Database> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Database> buildedDatabases = new ArrayList<>();

        if (keyValue.get("databaseUserId") != null) {
            for (Database database : repository.findByDatabaseUserIdsContaining(keyValue.get("databaseUserId"))) {
                buildedDatabases.add(construct(database));
            }
        } else if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            for (Database database : repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId"))) {
                buildedDatabases.add(construct(database));
            }
        } else if (keyValue.get("accountId") != null) {
            for (Database database : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedDatabases.add(construct(database));
            }
        } else if (keyValue.get("serviceId") != null) {
            for (Database database : repository.findByServiceId(keyValue.get("serviceId"))) {
                buildedDatabases.add(construct(database));
            }
        }

        return buildedDatabases;
    }

    @Override
    public Collection<Database> buildAll() {
        List<Database> buildedDatabases = new ArrayList<>();

        for (Database database : repository.findAll()) {
            buildedDatabases.add(construct(database));
        }

        return buildedDatabases;
    }

    @Override
    public void store(Database database) {
        repository.save(database);
    }

    public Count countByAccountId(String accountId) {
        Count count = new Count();
        count.setCount(repository.countByAccountId(accountId));
        return count;
    }

    public void updateQuota(String databaseId, Long quotaSize) {
        Database database = repository.findOne(databaseId);
        if (database != null) {
            database.setQuotaUsed(quotaSize);
        } else {
            throw new ResourceNotFoundException("Database с ID: " + databaseId + " не найден");
        }
        store(database);
    }

}
