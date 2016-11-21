package ru.majordomo.hms.rc.user.managers;

import org.bouncycastle.asn1.dvcs.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Database database = new Database();
        LordOfResources.setResourceParams(database, serviceMessage, cleaner);

        String serverId = cleaner.cleanString((String) serviceMessage.getParam("serverId"));
        if (serverId == null) {
            serverId = getActiveHostingServerId();
        }

        Long quota = (Long) serviceMessage.getParam("quota");
        Long quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
        Boolean writable = (Boolean) serviceMessage.getParam("writable");
        DBType type = (DBType) serviceMessage.getParam("type");
        List<String> databaseUserIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("databaseUserIds"));

        for (String databaseUserId: databaseUserIds) {
            DatabaseUser databaseUser = (DatabaseUser) governorOfDatabaseUser.build(databaseUserId);
            database.addDatabaseUser(databaseUser);
        }

        database.setServerId(serverId);
        database.setQuota(quota);
        database.setQuotaUsed(quotaUsed);
        database.setWritable(writable);
        database.setType(type);

        return database;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Database database = (Database) resource;

        if (database.getName().equals("")) {
            throw new ParameterValidateException("Имя базы не может быть пустым");
        }

        if (database.getSwitchedOn() == null) {
            throw new ParameterValidateException("Статус включен/выключен не определен");
        }

        if (database.getType() == null) {
            throw new ParameterValidateException("Тип базы не указан");
        }

        if (!serverExists(database.getServerId())) {
            throw new ParameterValidateException("Выбранный database сервер не существует");
        }

        if (database.getQuota() < 0) {
            throw new ParameterValidateException("Quota для базы не может быть меньше нуля");
        }

        if (database.getQuotaUsed() > database.getQuota()) {
            throw new ParameterValidateException("QuotaUsed не может быть больше quota");
        }

        if (database.getWritable() == null) {
            throw new ParameterValidateException("Флаг writable должен быть установлен");
        }

        DBType dbType = database.getType();
        for (DatabaseUser databaseUser: database.getDatabaseUsers()) {
            DBType userType = databaseUser.getType();
            String databaseUserId =databaseUser.getId();
            if (dbType != userType) {
                throw new ParameterValidateException("Тип базы: " + dbType +
                        ". Тип пользователя с ID:" + databaseUserId + " " + userType +
                        " Типы должны совпадать");
            }
        }
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

        Database database = new Database();

        if (hasResourceIdAndAccountId(keyValue)) {
            database = (Database) construct(repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId")));
        }

        return database;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {

        List<Database> buildedDatabases = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
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

    private String getActiveHostingServerId() {
        return staffRcClient.getActiveDatabaseServer().getId();
    }

    private boolean serverExists(String serverId) {
        return serverId != null && staffRcClient.getServerById(serverId) != null;
    }

}
