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
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfFTPUser extends LordOfResources<FTPUser> {

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
    public FTPUser create(ServiceMessage serviceMessage) throws ParameterValidateException {
        FTPUser ftpUser;
        try {
            ftpUser = buildResourceFromServiceMessage(serviceMessage);
            validate(ftpUser);
            store(ftpUser);
        } catch (ClassCastException | UnsupportedEncodingException e) {
            throw new ParameterValidateException("Один из параметров указан неверно" + e.getMessage());
        }

        return ftpUser;
    }

    @Override
    public FTPUser update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
        String resourceId;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidateException("Не указан resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        FTPUser ftpUser = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        ftpUser.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "homedir":
                        String homedir = cleaner.cleanString((String) entry.getValue());
                        if (homedir != null && homedir.startsWith("/")) {
                            homedir = homedir.substring(1);
                        }
                        ftpUser.setHomeDir(homedir);
                        break;
                    case "allowedIPAddresses":
                        try {
                            ftpUser.setAllowedIpsAsCollectionOfString(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        } catch (NumberFormatException e) {
                            throw new ParameterValidateException("Неверный формат IP-адреса");
                        }
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
    public void preDelete(String resourceId) {

    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository.findOne(resourceId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP с ID: " + resourceId + " не найден");
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected FTPUser buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
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
                if (homeDir != null && homeDir.startsWith("/")) {
                    homeDir = homeDir.substring(1);
                }
            }
            if (serviceMessage.getParam("unixAccountId") != null) {
                unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));
            }
            if (serviceMessage.getParam("allowedIPAddresses") != null) {
                allowedAddressListAsString = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("allowedIPAddresses"));
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
        try {
            ftpUser.setAllowedIpsAsCollectionOfString(allowedAddressListAsString);
        } catch (NumberFormatException e) {
            throw new ParameterValidateException("Неверный формат IP-адреса");
        }

        return ftpUser;
    }

    @Override
    public void validate(FTPUser ftpUser) throws ParameterValidateException {
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
            ftpUser.setHomeDir("");
        }
    }

    @Override
    protected FTPUser construct(FTPUser ftpUser) throws ResourceNotFoundException {
        UnixAccount unixAccount = governorOfUnixAccount.build(ftpUser.getUnixAccountId());
        ftpUser.setUnixAccount(unixAccount);
        return ftpUser;
    }

    @Override
    public FTPUser build(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository.findOne(resourceId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP с ID:" + resourceId + " не найден");
        }

        return construct(ftpUser);
    }

    @Override
    public FTPUser build(Map<String, String> keyValue) throws ResourceNotFoundException {
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
    public Collection<FTPUser> buildAll(Map<String, String> keyValue) throws ParameterValidateException {
        List<FTPUser> buildedFTPUsers = new ArrayList<>();

        boolean byAccountId = false;
        boolean byUnixAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
            if (entry.getKey().equals("unixAccountId")) {
                byUnixAccountId = true;
            }
        }

        if (byAccountId) {
            for (FTPUser ftpUser : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedFTPUsers.add(construct(ftpUser));
            }
        } else if (byUnixAccountId) {
            for (FTPUser ftpUser : repository.findByUnixAccountId(keyValue.get("unixAccountId"))) {
                buildedFTPUsers.add(construct(ftpUser));
            }
        }

        return buildedFTPUsers;
    }

    @Override
    public Collection<FTPUser> buildAll() {
        List<FTPUser> buildedFTPUsers = new ArrayList<>();
        for (FTPUser ftpUser: repository.findAll()) {
            buildedFTPUsers.add(construct(ftpUser));
        }
        return buildedFTPUsers;
    }

    @Override
    public void store(FTPUser ftpUser) {
        repository.save(ftpUser);
    }

    public Count countByAccountId(String accountId) {
        Count count = new Count();
        count.setCount(repository.countByAccountId(accountId));
        return count;
    }
}
