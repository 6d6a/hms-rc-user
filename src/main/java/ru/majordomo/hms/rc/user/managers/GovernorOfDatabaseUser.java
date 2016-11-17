package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Resource;

@Service
public class GovernorOfDatabaseUser extends LordOfResources {
    private Cleaner cleaner;
    private DatabaseUserRepository repository;

    @Autowired
    public void setRepository(DatabaseUserRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        DatabaseUser databaseUser;
        try {
            databaseUser = (DatabaseUser) buildResourceFromServiceMessage(serviceMessage);
            validate(databaseUser);
            store(databaseUser);
        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidateException("В пароле используются некорретные символы");
        }
        return null;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) != null) {
            repository.delete(resourceId);
        } else {
            throw new ResourceNotFoundException("Ресурс не найден");
        }
    }

    public void dropByAccountId(String resourceId, String accountId) throws ResourceNotFoundException {
        if (repository.findByIdAndAccountId(resourceId, accountId) != null) {
            repository.delete(resourceId);
        } else {
            throw new ResourceNotFoundException("Ресурс не найден");
        }
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        DatabaseUser databaseUser = new DatabaseUser();
        LordOfResources.setResourceParams(databaseUser, serviceMessage, cleaner);
        String password = cleaner.cleanString((String) serviceMessage.getParam("password"));

        DBType userType = null;
        String userTypeAsString;
        if (serviceMessage.getParam("type") != null) {
            userTypeAsString = cleaner.cleanString((String) serviceMessage.getParam("type"));
            userType = Enum.valueOf(DBType.class, userTypeAsString);
        }

        databaseUser.setType(userType);
        databaseUser.setPasswordHashByPlainPassword(password);

        return databaseUser;
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

        if (databaseUser.getPasswordHash() == null) {
            throw new ParameterValidateException("Пароль не может быть пустым");
        }

        if (databaseUser.getType() == null) {
            throw new ParameterValidateException("Тип не может быть пустым");
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
        DatabaseUser databaseUser = new DatabaseUser();

        boolean byAccountId = false;
        boolean byId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("databaseUserId")) {
                byId = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId && byId) {
            databaseUser = repository.findByIdAndAccountId(keyValue.get("databaseUserId"), keyValue.get("accountId"));

            if (databaseUser == null) {
                throw new ResourceNotFoundException("Пользователь баз данных с ID:" + keyValue.get("websiteId") + " и account ID:" + keyValue.get("accountId") + " не найден");
            }
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
