package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.common.Utils;
import ru.majordomo.hms.rc.user.configurations.MysqlSessionVariablesConfig;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.DatabaseUserImportChecks;

import static java.lang.String.join;

@Component
public class GovernorOfDatabaseUser extends LordOfResources<DatabaseUser> {
    private Cleaner cleaner;
    private DatabaseUserRepository repository;
    private GovernorOfDatabase governorOfDatabase;
    private Validator validator;
    private String defaultServiceName;
    private StaffResourceControllerClient staffRcClient;
    private MysqlSessionVariablesConfig mysqlSessionVariablesConfig;

    public GovernorOfDatabaseUser(OperationOversightRepository<DatabaseUser> operationOversightRepository) {
        super(operationOversightRepository);
    }

    @Value("${default.database.serviceName}")
    public void setDefaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
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
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Autowired
    public void setMysqlSessionVariablesConfig(MysqlSessionVariablesConfig config) {
        this.mysqlSessionVariablesConfig = config;
    }

    @Override
    public OperationOversight<DatabaseUser> createByOversight(ServiceMessage serviceMessage) throws ParameterValidationException {
        OperationOversight<DatabaseUser> ovs;

        DatabaseUser databaseUser;
        try {
            databaseUser = buildResourceFromServiceMessage(serviceMessage);
        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidationException("В пароле используются некорретные символы");
        }
        preValidate(databaseUser);
        Boolean replace = Boolean.TRUE.equals(serviceMessage.getParam("replaceOldResource"));
        validate(databaseUser);

        preValidate(databaseUser);

        if (databaseUser.getId() == null) {
            databaseUser.setId(new ObjectId().toString());
        }

        //Изменение DatabaseUserIds в базах произойдёт после ответа из TE на основании affectedResources (affectedDatabases) из OVS
        List<Database> affectedDatabases = new ArrayList<>();

        if (databaseUser.getDatabaseIds() != null && !databaseUser.getDatabaseIds().isEmpty()) {
            for (String databaseId : databaseUser.getDatabaseIds()) {
                Database database = governorOfDatabase.build(databaseId);
                database.addDatabaseUserId(databaseUser.getId());
                database.addDatabaseUser(databaseUser);
                affectedDatabases.add(database);
            }
        }

        ovs = sendToOversight(databaseUser, ResourceAction.CREATE, replace, affectedDatabases);

        return ovs;
    }

    @Override
    public OperationOversight<DatabaseUser> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException {
        DatabaseUser databaseUser = this.updateWrapper(serviceMessage);

        //При апдейте DatabaseUser изменения DatabaseUserIds не происходит, но сущность affectedResources всегда необходима в TE
        List<Database> affectedDatabases = governorOfDatabase.getDatabasesByDatabaseUserId(databaseUser.getId());

        return sendToOversight(databaseUser, ResourceAction.UPDATE, false, affectedDatabases);
    }

    private DatabaseUser updateWrapper(ServiceMessage serviceMessage) throws UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        DatabaseUser databaseUser = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        databaseUser.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "allowedAddressList":
                        try {
                            databaseUser.setAllowedIpsAsCollectionOfString(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        } catch (NumberFormatException e) {
                            throw new ParameterValidationException("Неверный формат IP-адреса");
                        }
                        break;
                    case "switchedOn":
                        databaseUser.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "serviceId":
                        databaseUser.setServiceId((String) entry.getValue());
                        break;
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            databaseUser.setWillBeDeletedAfter(null);
                        } else {
                            databaseUser.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    case "maxCpuTimePerSecond":
                        BigDecimal maxCpuTimePerSecond = Utils.getBigDecimalFromUnexpectedInput(
                                serviceMessage.getParam("maxCpuTimePerSecond")
                        );
                        databaseUser.setMaxCpuTimePerSecond(maxCpuTimePerSecond);

                        break;
                    case "sessionVariables":
                        setSessionVariables(
                                (Map<String, Object>) entry.getValue(),
                                databaseUser,
                                mysqlSessionVariablesConfig
                        );

                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(databaseUser);
        validate(databaseUser);

        return databaseUser;
    }

    /**
     * Создание/Изменение ресурса и последущие удаление Oversight
     */
    @Override
    public DatabaseUser completeOversightAndStore(OperationOversight<DatabaseUser> ovs) {
        if (ovs.getReplace()) {
            removeOldResource(ovs.getResource());
        }
        DatabaseUser databaseUser = ovs.getResource();
        store(databaseUser);

        if (ovs.getAffectedResources() != null && !ovs.getAffectedResources().isEmpty()) {
            ovs.getAffectedResources().forEach(item -> governorOfDatabase.store((Database) item));
        }

        removeOversight(ovs);
        return databaseUser;
    }

    @Override
    public void preDelete(String resourceId) {
        governorOfDatabase.removeDatabaseUserIdFromDatabases(resourceId);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    @Override
    public OperationOversight<DatabaseUser> dropByOversight(String resourceId) throws ResourceNotFoundException {
        DatabaseUser databaseUser = build(resourceId);

        List<Database> affectedDatabases = governorOfDatabase.preRemoveDatabaseUserIdFromDatabases(resourceId);

        return sendToOversight(databaseUser, ResourceAction.DELETE, false, affectedDatabases);
    }

    @Override
    public DatabaseUser buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException, UnsupportedEncodingException {
        DatabaseUser databaseUser = new DatabaseUser();
        setResourceParams(databaseUser, serviceMessage, cleaner);
        String password = null;
        String passwordHash = "";
        DBType userType = null;
        String serviceId = null;
        String userTypeAsString;
        List<String> allowedIps = null;
        List<String> databaseIds = null;

        try {
            if (serviceMessage.getParam("password") != null) {
                password = cleaner.cleanString((String) serviceMessage.getParam("password"));
            }
            if (serviceMessage.getParam("passwordHash") != null) {
                passwordHash = cleaner.cleanString((String) serviceMessage.getParam("passwordHash"));
            }
            if (serviceMessage.getParam("type") != null) {
                userTypeAsString = cleaner.cleanString((String) serviceMessage.getParam("type"));
                try {
                    userType = Enum.valueOf(DBType.class, userTypeAsString);
                    databaseUser.setType(userType);
                } catch (IllegalArgumentException e) {
                    throw new ParameterValidationException("Недопустимый тип баз данных");
                }
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

            if (serviceMessage.getParams().containsKey("maxCpuTimePerSecond")) {
                BigDecimal maxCpuTimePerSecond = null;
                try {
                    maxCpuTimePerSecond = Utils.getBigDecimalFromUnexpectedInput(serviceMessage.getParam("maxCpuTimePerSecond"));
                } catch (ParameterValidationException ignore) {} //this means that there is no limit
                databaseUser.setMaxCpuTimePerSecond(maxCpuTimePerSecond);
            }

            if (serviceMessage.getParam("sessionVariables") instanceof Map) {
                setSessionVariables(
                        (Map<String, Object>) serviceMessage.getParam("sessionVariables"),
                        databaseUser,
                        mysqlSessionVariablesConfig
                );
            }

        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        databaseUser.setDatabaseIds(databaseIds);
        databaseUser.setServiceId(serviceId);

        try {
            if (StringUtils.isNotEmpty(passwordHash)) {
                databaseUser.setPasswordHash(passwordHash);
            } else {
                databaseUser.setPasswordHashByPlainPassword(password);
            }
        } catch (IllegalArgumentException e) {
            throw new ParameterValidationException(e.getMessage());
        }
        try {
            databaseUser.setAllowedIpsAsCollectionOfString(allowedIps);
        } catch (NumberFormatException e) {
            throw new ParameterValidationException("Неверный формат IP-адреса");
        }
        return databaseUser;
    }

    @Override
    public void preValidate(DatabaseUser databaseUser) {
        preValidateDatabaseServiceId(databaseUser, staffRcClient, defaultServiceName);
    }

    @Override
    public void validate(DatabaseUser databaseUser) throws ParameterValidationException {
        Set<ConstraintViolation<DatabaseUser>> constraintViolations = validator.validate(databaseUser, DatabaseUserChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("databaseUser: " + databaseUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(DatabaseUser databaseUser) {
        Set<ConstraintViolation<DatabaseUser>> constraintViolations = validator.validate(databaseUser, DatabaseUserImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] databaseUser: " + databaseUser + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected DatabaseUser construct(DatabaseUser databaseUser) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseUser build(String resourceId) throws ResourceNotFoundException {
        DatabaseUser resource = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь баз данных с ID: " + resourceId + " не найден"));

        return resource;
    }

    @Override
    public DatabaseUser build(Map<String, String> keyValue) throws ResourceNotFoundException {
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
    protected void removeOldResource(DatabaseUser resource) {
        if (resource == null || StringUtils.isEmpty(resource.getAccountId()) || StringUtils.isEmpty(resource.getName())) {
            return;
        }
        repository.deleteByAccountIdAndName(resource.getAccountId(), resource.getName());
    }

    @Override
    public Collection<DatabaseUser> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<DatabaseUser> buildedDatabasesUsers = new ArrayList<>();

        if (keyValue.get("accountId") != null && keyValue.get("serviceId") != null) {
            buildedDatabasesUsers.addAll(repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId")));
        } else if (keyValue.get("accountId") != null) {
            buildedDatabasesUsers = repository.findByAccountId(keyValue.get("accountId"));
        } else if (keyValue.get("serviceId") != null) {
            buildedDatabasesUsers.addAll(repository.findByServiceId(keyValue.get("serviceId")));
        }

        return buildedDatabasesUsers;
    }

    @Override
    public Collection<DatabaseUser> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(DatabaseUser databaseUser) {
        repository.save(databaseUser);
    }

    private void setSessionVariables(Map<String, Object> sessionVariables, DatabaseUser databaseUser, MysqlSessionVariablesConfig cnf) {
        if (databaseUser == null || databaseUser.getType() == null) {
            return;
        }
        switch (databaseUser.getType()) {
            case MYSQL:
                (sessionVariables).forEach((key, value) -> {
                    if (value == null) {
                        databaseUser.getSessionVariables().remove(key);
                    } else {
                        if (Arrays.asList("characterSetClient", "characterSetResults", "characterSetConnection").contains(key)) {
                            if (value instanceof String && cnf.getCharsets().contains(value)) {
                                databaseUser.getSessionVariables().put(key, value);
                            } else {
                                throw new ParameterValidationException(
                                        "Значение " + key + " должно быть одиним из " + join(", ", cnf.getCharsets())
                                );
                            }
                        } else if ("collationConnection".equals(key)) {
                            if (value instanceof String && cnf.getCollations().contains(value)) {
                                databaseUser.getSessionVariables().put(key, value);
                            } else {
                                throw new ParameterValidationException(
                                        "Значение " + key + " должно быть одиним из " + join(", ", cnf.getCollations())
                                );
                            }
                        } else if ("queryCacheType".equals(key)) {
                            if (value instanceof String && cnf.getQueryCacheTypes().contains(value)) {
                                databaseUser.getSessionVariables().put(key, value);
                            } else {
                                throw new ParameterValidationException(
                                        "Значение " + key + " должно быть одиним из " + join(", ", cnf.getQueryCacheTypes())
                                );
                            }
                        } else if ("innodbStrictMode".equals(key)) {
                            if (value instanceof String && cnf.getInnodbStrictMode().contains(value)) {
                                databaseUser.getSessionVariables().put(key, value);
                            } else {
                                throw new ParameterValidationException(
                                        "Значение " + key + " должно быть одиним из " + join(", ", cnf.getInnodbStrictMode())
                                );
                            }
                        }
                    }
                });

                break;
            case POSTGRES:
            default:
                break;
        }
    }
}
