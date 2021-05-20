package ru.majordomo.hms.rc.user.managers;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.staff.resources.Resource;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseImportChecks;

@Component
public class GovernorOfDatabase extends LordOfResources<Database> {

    private DatabaseRepository repository;
    private Cleaner cleaner;
    private GovernorOfDatabaseUser governorOfDatabaseUser;
    private GovernorOfResourceArchive governorOfResourceArchive;
    private Validator validator;
    private String defaultServiceName;
    private StaffResourceControllerClient staffRcClient;
    private MongoClient mongoClient;
    private String springDataMongodbDatabase;

    public GovernorOfDatabase(OperationOversightRepository<Database> operationOversightRepository) {
        super(operationOversightRepository);
    }

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

    @Autowired
    public void setMongoClient(@Qualifier("jongoMongoClient") MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    protected void removeOldResource(Database resource) {
        if (resource == null || StringUtils.isEmpty(resource.getAccountId()) || StringUtils.isEmpty(resource.getName())) {
            return;
        }
        repository.deleteByAccountIdAndName(resource.getAccountId(), resource.getName());
    }

    @Value("${spring.data.mongodb.database}")
    public void setSpringDataMongodbDatabase(String springDataMongodbDatabase) {
        this.springDataMongodbDatabase = springDataMongodbDatabase;
    }

    @Override
    public OperationOversight<Database> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException {
        Database database = this.updateWrapper(serviceMessage);

        return sendToOversight(database, ResourceAction.UPDATE);
    }

    private Database updateWrapper(ServiceMessage serviceMessage) {
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
                    case "writable":
                        database.setWritable((Boolean) entry.getValue());
                        break;
                    case "serviceId":
                        database.setServiceId((String) entry.getValue());
                        break;
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            database.setWillBeDeletedAfter(null);
                        } else {
                            database.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(database);
        validate(database);

        return database;
    }

    public void removeDatabaseUserIdFromDatabases(String databaseUserId) {
        List<Database> databases = repository.findByDatabaseUserIdsContaining(databaseUserId);
        for (Database database : databases) {
            List<String> databaseUserIds = database.getDatabaseUserIds();
            databaseUserIds.remove(databaseUserId);
            database.setDatabaseUserIds(databaseUserIds);
        }
        repository.saveAll(databases);
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfResourceArchive.dropByArchivedResourceId(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Database c ID: " + resourceId + " не найден");
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    @Override
    public OperationOversight<Database> dropByOversight(String resourceId) throws ResourceNotFoundException {
        Database database = build(resourceId);
        return sendToOversight(database, ResourceAction.DELETE);
    }

    @Override
    public Database buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
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
    public void validate(Database database) throws ParameterValidationException {
        Set<ConstraintViolation<Database>> constraintViolations = validator.validate(database, DatabaseChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("database: " + database + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Database database) {
        Set<ConstraintViolation<Database>> constraintViolations = validator.validate(database, DatabaseImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] database: " + database + " constraintViolations: " + constraintViolations.toString());
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
        Database database = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("База данных с ID " + resourceId + " не найдена"));

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
        return new Count(repository.countByAccountId(accountId));
    }

    public void updateQuota(String databaseId, Long quotaSize) {
        Database database = repository.findById(databaseId).orElseThrow(() -> new ResourceNotFoundException("Database с ID: " + databaseId + " не найден"));

        database.setQuotaUsed(quotaSize);

        store(database);
    }

    public void processQuotaReport(ServiceMessage serviceMessage) {
        String name = null, host = null, serviceType = null;
        Long quotaUsed = null;

        if (serviceMessage.getParam("db") != null) {
            name = (String) serviceMessage.getParam("db");
        }

        if (serviceMessage.getParam("host") != null) {
            host = (String) serviceMessage.getParam("host");
        }

        if (serviceMessage.getParam("serviceType") != null) {
            serviceType = (String) serviceMessage.getParam("serviceType");
        }

        if (serviceMessage.getParam("quotaUsed") != null) {
            Object quotaUsedFromMessage = serviceMessage.getParam("quotaUsed");
            if (quotaUsedFromMessage instanceof Long) {
                quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
            } else if (quotaUsedFromMessage instanceof Integer) {
                quotaUsed = ((Integer) serviceMessage.getParam("quotaUsed")).longValue();
            }
        }

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);

        MongoCollection databasesCollection = jongo.getCollection("databases");

        if (name != null && host != null && serviceType != null && quotaUsed != null) {
            List<Server> servers = staffRcClient.getCachedServersOnlyIdAndNameByName(host);
            if (!servers.isEmpty()) {
                List<Service> services = staffRcClient.getCachedServicesByServerIdAndServiceType(servers.get(0).getId(), serviceType);

                if (!services.isEmpty()) {
                    Database currentDatabase = databasesCollection
                            .findOne("{name: #, serviceId: {$in: #}}", name, services.stream().map(Resource::getId).collect(Collectors.toList()))
                            .projection("{quotaUsed: 1}")
                            .map(
                                    result -> {
                                        Database database = new Database();

                                        if (result.get("_id") instanceof ObjectId) {
                                            database.setId(((ObjectId) result.get("_id")).toString());
                                        } else if (result.get("_id") instanceof String) {
                                            database.setId((String) result.get("_id"));
                                        }

                                        database.setQuotaUsed((Long) result.get("quotaUsed"));
                                        return database;
                                    }
                            );

                    if (currentDatabase != null && !currentDatabase.getQuotaUsed().equals(quotaUsed)) {
                        log.info("databases quotaReport for host '" + host + "' and name '" + name + "' found changed quotaUsed. old: " + currentDatabase.getQuotaUsed() + " new: " + quotaUsed);

                        Object objectId = currentDatabase.getId();

                        try {
                            objectId = new ObjectId(currentDatabase.getId());
                        } catch (Exception ignored) {}

                        WriteResult writeResult = databasesCollection.update("{_id: #}", objectId).with("{$set: {quotaUsed: #}}", quotaUsed);
                    }
                }
            }
        }
    }
}
