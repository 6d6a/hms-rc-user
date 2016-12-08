package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

import ru.majordomo.hms.rc.user.api.DTO.Count;
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
    public Resource update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidateException("Не указан resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        FTPUser ftpUser = (FTPUser) build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        ftpUser.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "homedir":
                        ftpUser.setHomeDir(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "allowedAddressList":
                        ftpUser.setAllowedIpsAsCollectionOfString(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "switchedOn":
                        ftpUser.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validate(ftpUser);
        store(ftpUser);

        return ftpUser;
    }

    private Boolean hasUniqueName(String name) {
        return (repository.findOneByName(name) == null);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository.findOne(resourceId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP с ID: " + resourceId + " не найден");
        }
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        FTPUser ftpUser = new FTPUser();
        LordOfResources.setResourceParams(ftpUser, serviceMessage, cleaner);

        String plainPassword = "";
        String homeDir = null;
        String unixAccountId = null;
        List<String> allowedAddressListAsString = new ArrayList<>();

        try {
            if (serviceMessage.getParam("password") != null) {
                plainPassword = cleaner.cleanString((String) serviceMessage.getParam("password"));
            }
            if (serviceMessage.getParam("homedir") != null) {
                homeDir = cleaner.cleanString((String) serviceMessage.getParam("homedir"));
            }
            if (serviceMessage.getParam("unixAccountId") != null) {
                unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));
            }
            if (serviceMessage.getParam("allowedAddressList") != null) {
                allowedAddressListAsString = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("allowedAddressList"));
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        if (!hasUniqueName(ftpUser.getName())) {
            throw new ParameterValidateException("Имя пользователя существует в системе");
        }

        ftpUser.setPasswordHashByPlainPassword(plainPassword);
        ftpUser.setHomeDir(homeDir);
        ftpUser.setUnixAccountId(unixAccountId);
        ftpUser.setAllowedIpsAsCollectionOfString(allowedAddressListAsString);

        return ftpUser;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        FTPUser ftpUser = (FTPUser) resource;
        if (ftpUser.getName() == null) {
            throw new ParameterValidateException("Имя FTP пользователя не может быть пустым");
        }

        if (ftpUser.getPasswordHash() == null || ftpUser.getPasswordHash().equals("")) {
            throw new ParameterValidateException("Пароль FTP пользователя не может быть пустым");
        }

        if (ftpUser.getUnixAccountId() == null) {
            throw new ParameterValidateException("Параметр unixAccount не может быть пустым");
        }

        try {
            governorOfUnixAccount.build(ftpUser.getUnixAccountId());
        } catch (ResourceNotFoundException e) {
            throw new ParameterValidateException("Не найден UnixAccount с ID: " + ftpUser.getUnixAccountId());
        }

        if (ftpUser.getHomeDir() == null) {
            throw new ParameterValidateException("Домашняя директория FTP пользователя должна быть указана");
        }
    }

    @Override
    protected Resource construct(Resource resource) throws ResourceNotFoundException {
        FTPUser ftpUser = (FTPUser) resource;
        UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(ftpUser.getUnixAccountId());
        ftpUser.setUnixAccount(unixAccount);
        return ftpUser;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository.findOne(resourceId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP с ID:" + resourceId + " не найден");
        }

        return construct(ftpUser);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        FTPUser ftpUser = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            ftpUser = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (keyValue.get("name") != null && !keyValue.get("name").equals("")) {
            ftpUser = repository.findOneByName(keyValue.get("name"));
        }

        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP не найден");
        }

        return construct(ftpUser);
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ParameterValidateException {
        List<FTPUser> buildedFTPUsers = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            for (FTPUser ftpUser : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedFTPUsers.add((FTPUser) construct(ftpUser));
            }
        }

        return buildedFTPUsers;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<FTPUser> buildedFTPUsers = new ArrayList<>();
        for (FTPUser ftpUser: repository.findAll()) {
            buildedFTPUsers.add((FTPUser) construct(ftpUser));
        }
        return buildedFTPUsers;
    }

    @Override
    public void store(Resource resource) {
        FTPUser ftpUser = (FTPUser) resource;
        repository.save(ftpUser);
    }

    public Count countByAccountId(String accountId) {
        Count count = new Count();
        count.setCount(repository.countByAccountId(accountId));
        return count;
    }
}
