package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.validation.group.FTPUserChecks;

import static ru.majordomo.hms.rc.user.common.Utils.mapContains;

@Service
public class GovernorOfFTPUser extends LordOfResources<FTPUser> {

    private Cleaner cleaner;
    private FTPUserRepository repository;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Validator validator;

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

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public FTPUser update(ServiceMessage serviceMessage)
            throws ParameterValidationException, UnsupportedEncodingException {
        String resourceId;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        } else {
            throw new ParameterValidationException("Не указан resourceId");
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
                        ftpUser.setAllowedIPAddresses(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "allowWebFtp":
                        try {
                            ftpUser.setAllowWebFtp((Boolean) entry.getValue());
                        } catch (Exception e) {
                            throw new ParameterValidationException("Неверный формат allowWebFtp");
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
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        validate(ftpUser);
        store(ftpUser);

        return ftpUser;
    }

    @Override
    protected void removeOldResource(FTPUser resource) {
        if (resource == null || StringUtils.isEmpty(resource.getAccountId()) || StringUtils.isEmpty(resource.getName())) {
            return;
        }
        repository.deleteByNameAndAccountId(resource.getName(), resource.getAccountId());
    }

    @Override
    public void preDelete(String resourceId) {

    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        FTPUser ftpUser = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь FTP с ID: " + resourceId + " не найден"));

        preDelete(resourceId);

        repository.deleteById(resourceId);
    }

    @Override
    public FTPUser buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        FTPUser ftpUser = new FTPUser();
        setResourceParams(ftpUser, serviceMessage, cleaner);

        String plainPassword = "";
        String passwordHash = "";
        String homeDir = null;
        String unixAccountId = null;
        boolean switchedOn = true;
        boolean allowWebFtp = true;

        try {
            if (serviceMessage.getParam("passwordHash") != null) {
                passwordHash = cleaner.cleanString((String) serviceMessage.getParam("passwordHash"));
            }
            if (serviceMessage.getParam("password") != null) {
                plainPassword = cleaner.cleanString((String) serviceMessage.getParam("password"));
            }
            if (serviceMessage.getParam("homedir") != null) {
                homeDir = cleaner.cleanString((String) serviceMessage.getParam("homedir"));
                if (homeDir != null && homeDir.startsWith("/")) {
                    homeDir = homeDir.substring(1);
                }
            }
            if (Boolean.FALSE.equals(serviceMessage.getParam("switchedOn"))) {
                switchedOn = false;
            }
            if (Boolean.FALSE.equals(serviceMessage.getParam("allowWebFtp"))) {
                allowWebFtp = false;
            }
            if (serviceMessage.getParam("unixAccountId") != null) {
                unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));
            }
            if (serviceMessage.getParam("allowedIPAddresses") != null) {
                ftpUser.setAllowedIPAddresses(
                        cleaner.cleanListWithStrings(
                                (List<String>) serviceMessage.getParam("allowedIPAddresses")
                        )
                );
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        if (StringUtils.isNotEmpty(passwordHash)) {
            ftpUser.setPasswordHash(passwordHash);
        } else {
            ftpUser.setPasswordHashByPlainPassword(plainPassword);
        }
        ftpUser.setHomeDir(homeDir);
        ftpUser.setUnixAccountId(unixAccountId);
        ftpUser.setAllowWebFtp(allowWebFtp);
        ftpUser.setSwitchedOn(switchedOn);

        return ftpUser;
    }

    @Override
    public void validate(FTPUser ftpUser) throws ParameterValidationException {
        Set<ConstraintViolation<FTPUser>> constraintViolations = validator.validate(ftpUser, FTPUserChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("ftpUser: " + ftpUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(FTPUser ftpUser) {
        Set<ConstraintViolation<FTPUser>> constraintViolations = validator.validate(ftpUser, FTPUserChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] ftpUser: " + ftpUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
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
        FTPUser ftpUser = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь FTP с ID:" + resourceId + " не найден"));

        return construct(ftpUser);
    }

    @Override
    public FTPUser build(Map<String, String> keyValue) throws ResourceNotFoundException {
        FTPUser ftpUser = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            ftpUser = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (keyValue.get("name") != null && !keyValue.get("name").equals("")) {
            if (mapContains(keyValue, "accountId")) {
                ftpUser = repository.findByNameAndAccountId(keyValue.get("name"), keyValue.get("accountId"));
            } else {
                ftpUser = repository.findOneByName(keyValue.get("name"));
            }
        }

        if (ftpUser == null) {
            throw new ResourceNotFoundException("Пользователь FTP не найден");
        }

        return construct(ftpUser);
    }

    @Override
    public Collection<FTPUser> buildAll(Map<String, String> keyValue) throws ParameterValidationException {
        List<FTPUser> buildedFTPUsers = new ArrayList<>();

        if (keyValue.get("accountId") != null) {
            for (FTPUser ftpUser : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedFTPUsers.add(construct(ftpUser));
            }
        } else if (keyValue.get("unixAccountId") != null) {
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
        return new Count(repository.countByAccountId(accountId));
    }
}
