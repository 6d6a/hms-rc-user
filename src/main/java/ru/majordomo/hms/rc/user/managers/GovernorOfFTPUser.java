package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfFTPUser extends LordOfResources {

    private Cleaner cleaner;
    private FTPUserRepository repository;
    private GovernorOfUnixAccount governorOfUnixAccount;

    @Autowired
    public void setGovernorOfUnixAccount(GovernorOfUnixAccount governorOfUnixAccount) {
        this.governorOfUnixAccount = governorOfUnixAccount;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setRepository(FTPUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        FTPUser ftpUser;
        try {
            ftpUser = (FTPUser) buildResourceFromServiceMessage(serviceMessage);
            validate(ftpUser);
            store(ftpUser);
        } catch (ClassCastException | UnsupportedEncodingException e) {
            throw new ParameterValidateException("Один из параметров указан неверно" + e.getMessage());
        }

        return ftpUser;
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
        FTPUser ftpUser = new FTPUser();
        LordOfResources.setResourceParams(ftpUser, serviceMessage, cleaner);
        String plainPassword = cleaner.cleanString((String) serviceMessage.getParam("password"));
        String homeDir = cleaner.cleanString((String) serviceMessage.getParam("homedir"));
        String unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));

        ftpUser.setPasswordHashByPlainPassword(plainPassword);
        ftpUser.setHomeDir(homeDir);
        ftpUser.setUnixAccount((UnixAccount) governorOfUnixAccount.build(unixAccountId));

        return ftpUser;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        FTPUser ftpUser = (FTPUser) resource;
        if (ftpUser.getName() == null) {
            throw new ParameterValidateException("Имя FTP пользователя не может быть пустым");
        }

        if (ftpUser.getPasswordHash() == null) {
            throw new ParameterValidateException("Пароль FTP пользователя не может быть пустым");
        }

        if (ftpUser.getHomeDir() == null) {
            throw new ParameterValidateException("Домашняя директория FTP пользователя должна быть указана");
        }

        if (ftpUser.getUnixAccount() == null) {
            throw new ParameterValidateException("Параметр unixAccount не может быть пустым");
        }
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository.findOne(resourceId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь с ID:" + resourceId + " не найден");
        }

        return ftpUser;
    }

    @Override
    public Collection<? extends Resource> buildByAccount(String accountId) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<FTPUser> ftpUsers = repository.findAll();

        return ftpUsers;
    }

    @Override
    public void store(Resource resource) {
        FTPUser ftpUser = (FTPUser) resource;
        repository.save(ftpUser);
    }
}
