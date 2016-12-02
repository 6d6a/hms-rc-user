package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.*;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Resource;

@Component
public class GovernorOfDatabaseUser extends LordOfResources {
    private Cleaner cleaner;
    private DatabaseUserRepository repository;
    private GovernorOfDatabase governorOfDatabase;

    private StaffResourceControllerClient staffRcClient;
    private String defaultServiceName;

    @Value("${default.database.service.name}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
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
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        DatabaseUser databaseUser;
        try {
            databaseUser = (DatabaseUser) buildResourceFromServiceMessage(serviceMessage);
            validate(databaseUser);
            store(databaseUser);

            if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
                for (String databaseId : databaseUser.getDatabaseIds()) {
                    Database database = (Database) governorOfDatabase.build(databaseId);
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
    public Resource update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        DatabaseUser databaseUser = (DatabaseUser) build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        databaseUser.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "allowedAddressList":
                        databaseUser.setAllowedIpsAsCollectionOfString(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
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
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) != null) {
            repository.delete(resourceId);
            governorOfDatabase.removeDatabaseUserIdFromDatabases(resourceId);
        } else {
            throw new ResourceNotFoundException("Ресурс не найден");
        }
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException, UnsupportedEncodingException {
        DatabaseUser databaseUser = new DatabaseUser();
        LordOfResources.setResourceParams(databaseUser, serviceMessage, cleaner);
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
                userType = Enum.valueOf(DBType.class, userTypeAsString);
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

        if (!hasUniqueName(databaseUser.getName())) {
            throw new ParameterValidateException("Имя должно быть уникальным");
        }

        databaseUser.setDatabaseIds(databaseIds);
        databaseUser.setServiceId(serviceId);
        databaseUser.setType(userType);
        databaseUser.setPasswordHashByPlainPassword(password);
        databaseUser.setAllowedIpsAsCollectionOfString(allowedIps);


        return databaseUser;
    }

    private Boolean hasUniqueName(String name) {
        return (repository.findByName(name) == null);
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        DatabaseUser databaseUser = (DatabaseUser) resource;

        if (databaseUser.getAccountId() == null || databaseUser.getAccountId().equals("")) {
            throw new ParameterValidateException("AccountID не может быть пустым");
        }

        if (databaseUser.getName() == null) {
            throw new ParameterValidateException("Имя не может быть пустым");
        }

        if (databaseUser.getName().length() > 16) {
            throw new ParameterValidateException("Имя не может быть длиннее 16 символов");
        }

        if (databaseUser.getPasswordHash() == null) {
            throw new ParameterValidateException("Пароль не может быть пустым");
        }

        if (databaseUser.getSwitchedOn() == null) {
            databaseUser.setSwitchedOn(true);
        }

        if (databaseUser.getType() == null) {
            throw new ParameterValidateException("Тип не может быть пустым");
        }

        if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
            for (String databaseId : databaseUser.getDatabaseIds()) {
                Map<String, String> keyValue = new HashMap<>();
                keyValue.put("accountId", databaseUser.getAccountId());
                keyValue.put("resourceId", databaseId);
                try {
                    governorOfDatabase.build(keyValue);
                } catch (ResourceNotFoundException e) {
                    throw new ParameterValidateException("Не найдена база данных с ID: " + databaseId);
                }
            }
        }

        if (databaseUser.getServiceId() != null && !databaseUser.getServiceId().equals("")) {
            Server server = staffRcClient.getServerByServiceId(databaseUser.getServiceId());
            if (server == null) {
                throw new ParameterValidateException("Не найден сервис с ID: " + databaseUser.getServiceId());
            }
        } else {
            String serverId = staffRcClient.getActiveDatabaseServer().getId();

            List<Service> databaseServices = staffRcClient.getDatabaseServicesByServerIdAndServiceType(serverId);
            if (databaseServices != null) {
                for (Service service : databaseServices) {
                    if (service.getServiceType().getName().equals(this.defaultServiceName)) {
                        databaseUser.setServiceId(service.getId());
                        break;
                    }
                }
                if (databaseUser.getServiceId() == null || (databaseUser.getServiceId().equals(""))) {
                    throw new ParameterValidateException("Не найдено serviceType: " + this.defaultServiceName +
                            " для сервера: " + serverId);
                }
            }
        }
    }

    @Override
    protected Resource construct(Resource resource) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Resource resource = repository.findOne(resourceId);
        if (resource != null) {
            return resource;
        } else {
            throw new ResourceNotFoundException("Пользователь баз данных с ID: " + resourceId + " не найден");
        }
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
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
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {

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
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        DatabaseUser databaseUser = (DatabaseUser) resource;
        repository.save(databaseUser);
    }
}
