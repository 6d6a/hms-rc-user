package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

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

    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        DatabaseUser databaseUser = new DatabaseUser();
        LordOfResources.setResourceParams(databaseUser, serviceMessage, cleaner);
        String password = cleaner.cleanString((String) serviceMessage.getParam("password"));
        DBType userType = (DBType) serviceMessage.getParam("type");

        databaseUser.setPasswordHashByPlainPassword(password);
        databaseUser.setType(userType);

        return databaseUser;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        DatabaseUser databaseUser = (DatabaseUser) resource;
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
    public Resource build(String resourceId) throws ResourceNotFoundException {
        return repository.findOne(resourceId);
    }

    @Override
    public Collection<? extends Resource> buildByAccount(String accountId) throws NotImplementedException {
        throw new NotImplementedException();
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
