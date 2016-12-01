package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.resources.Database;

@Service
public class GovernorOfDatabase extends LordOfResources {

    private DatabaseRepository repository;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;
    private GovernorOfDatabaseUser governorOfDatabaseUser;
    private String defaultServiceName;

    @Value("${default.database.service.name}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Autowired
    public void setGovernorOfDatabaseUser(GovernorOfDatabaseUser governorOfDatabaseUser) {
        this.governorOfDatabaseUser = governorOfDatabaseUser;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setRepository(DatabaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Database database;
        try {

            database = (Database) buildResourceFromServiceMessage(serviceMessage);
            validate(database);
            store(database);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return database;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Database database = (Database) build(keyValue);
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
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Database c ID: " + resourceId + " не найден");
        }
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Database database = new Database();
        String serviceId = null;
        DBType type = null;

        LordOfResources.setResourceParams(database, serviceMessage, cleaner);

        if (!hasUniqueName(database.getName())) {
            throw new ParameterValidateException("Имя базы данных занято");
        }

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

        List<String> databaseUserIds = null;
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
    public void validate(Resource resource) throws ParameterValidateException {
        Database database = (Database) resource;

        if (database.getAccountId() == null || database.getAccountId().equals("")) {
            throw new ParameterValidateException("Аккаунт ID не может быть пустым");
        }

        if (database.getName().equals("") || database.getName() == null) {
            throw new ParameterValidateException("Имя базы не может быть пустым");
        }

        if (database.getType() == null) {
            throw new ParameterValidateException("Тип базы не указан");
        }

        if (database.getServiceId() != null && !database.getServiceId().equals("")) {
            Server server = staffRcClient.getServerByServiceId(database.getServiceId());
            if (server == null) {
                throw new ParameterValidateException("Не найден сервис с ID: " + database.getServiceId());
            }
        } else {
            String serverId = staffRcClient.getActiveDatabaseServer().getId();

            List<ru.majordomo.hms.rc.staff.resources.Service> databaseServices = staffRcClient.getDatabaseServicesByServerIdAndServiceType(serverId);
            if (databaseServices != null) {
                for (ru.majordomo.hms.rc.staff.resources.Service service : databaseServices) {
                    if (service.getServiceType().getName().equals(this.defaultServiceName)) {
                        database.setServiceId(service.getId());
                        break;
                    }
                }
                if (database.getServiceId() == null || (database.getServiceId().equals(""))) {
                    throw new ParameterValidateException("Не найдено serviceType: " + this.defaultServiceName +
                            " для сервера: " + serverId);
                }
            }
        }

        DBType dbType = database.getType();
        for (DatabaseUser databaseUser: database.getDatabaseUsers()) {
            DBType userType = databaseUser.getType();
            if (dbType != userType) {
                throw new ParameterValidateException("Тип базы данных: " + dbType +
                        ". Тип пользователя с ID " + databaseUser.getId() + ": " + userType +
                        ". Типы должны совпадать");
            }
        }

        if (database.getSwitchedOn() == null) {
            database.setSwitchedOn(true);
        }

        if (database.getWritable() == null) {
            database.setWritable(true);
        }
    }

    private Boolean hasUniqueName(String name) {
        return (repository.findByName(name) == null);
    }

    @Override
    protected Resource construct(Resource resource) {
        Database database = (Database) resource;
        List<DatabaseUser> databaseUsers = new ArrayList<>();
        for ( String databaseUserId : database.getDatabaseUserIds()) {
            databaseUsers.add((DatabaseUser) governorOfDatabaseUser.build(databaseUserId));
        }
        database.setDatabaseUsers(databaseUsers);
        return database;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Database database = repository.findOne(resourceId);
        if (database == null) {
            throw new ResourceNotFoundException("Database с ID:" + resourceId + " не найдена");
        }
        return construct(database);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {

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
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {

        List<Database> buildedDatabases = new ArrayList<>();

        boolean byAccountId = false;
        boolean byDatabaseUserId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
            if (entry.getKey().equals("databaseUserId")) {
                byDatabaseUserId = true;
            }
        }

        if (byDatabaseUserId) {
            for (Database database : repository.findByDatabaseUserIdsContaining(keyValue.get("databaseUserId"))) {
                buildedDatabases.add((Database) construct(database));
            }
        } else if (byAccountId) {
            for (Database database : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedDatabases.add((Database) construct(database));
            }
        }

        return buildedDatabases;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<Database> buildedDatabases = new ArrayList<>();

        for (Database database : repository.findAll()) {
            buildedDatabases.add((Database) construct(database));
        }

        return buildedDatabases;
    }

    @Override
    public void store(Resource resource) {
        Database database = (Database) resource;
        repository.save(database);
    }

    public Count countByAccountId(String accountId) {
        Count count = new Count();
        count.setCount(repository.countByAccountId(accountId));
        return count;
    }

}
