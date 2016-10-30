package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.DBType;
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

        Double size = (Double) serviceMessage.getParam("size");
        DBType type = (DBType) serviceMessage.getParam("type");

        database.setServerId(serverId);
        database.setSize(size);
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

        if (database.getSize() < 0) {
            throw new ParameterValidateException("Размер базы не может быть меньше нуля");
        }
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Database database = repository.findOne(resourceId);
        if (database == null) {
            throw new ResourceNotFoundException("Database с ID:" + resourceId + " не найдена");
        }
        return database;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        Database database = (Database) resource;
        repository.save(database);
    }

    private String getActiveHostingServerId() {
        return staffRcClient.getActiveDatabaseServer().getId();
    }

    private boolean serverExists(String serverId) {
        return serverId != null && staffRcClient.getServerById(serverId) != null;
    }

}
