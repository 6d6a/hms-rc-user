package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.mongodb.*;

import lombok.Setter;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.common.SSHKeyManager;
import ru.majordomo.hms.rc.user.event.infect.UnixAccountInfectNotifyEvent;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.MalwareReportRepository;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.MalwareReport;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.validation.group.UnixAccountChecks;
import ru.majordomo.hms.rc.user.service.CounterService;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;

import static ru.majordomo.hms.rc.user.common.Utils.getLongFromUnexpectedInput;

@Component
public class GovernorOfUnixAccount extends LordOfResources<UnixAccount> {

    private UnixAccountRepository repository;
    private GovernorOfFTPUser governorOfFTPUser;
    private GovernorOfWebSite governorOfWebSite;
    private MalwareReportRepository malwareReportRepository;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;
    private Validator validator;
    private ApplicationEventPublisher publisher;
    private MongoOperations mongoOperations;
    private String springDataMongodbDatabase;
    private MongoClient mongoClient;
    private CounterService counterService;
    private PmFeignClient personmgr;

    @Setter
    @Nullable
    @Value("${resources.unixAccount.puttygenPath:puttygen}")
    private String puttygenPath;

    @Autowired
    public void setPmFeignClient (PmFeignClient personmgr) {
        this.personmgr = personmgr;
    }

    @Autowired
    public void setCounterService(CounterService counterService) {
        this.counterService = counterService;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setMongoOperations(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Autowired
    public void setRepository(UnixAccountRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setGovernorOfFTPUser(GovernorOfFTPUser governorOfFTPUser) {
        this.governorOfFTPUser = governorOfFTPUser;
    }

    @Autowired
    public void setGovernorOfWebSite(GovernorOfWebSite governorOfWebSite) {
        this.governorOfWebSite = governorOfWebSite;
    }

    @Autowired
    public void setMalwareReportRepository(MalwareReportRepository malwareReportRepository) {
        this.malwareReportRepository = malwareReportRepository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Value("${spring.data.mongodb.database}")
    public void setSpringDataMongodbDatabase(String springDataMongodbDatabase) {
        this.springDataMongodbDatabase = springDataMongodbDatabase;
    }

    @Autowired
    public void setMongoClient(@Qualifier("jongoMongoClient") MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public UnixAccount update(ServiceMessage serviceMessage) throws ParameterValidationException {
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

        UnixAccount unixAccount = build(keyValue);
        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "keyPair":
                        String value = cleaner.cleanString((String) entry.getValue());
                        if (value.equals("GENERATE")) {
                            try {
                                unixAccount.setKeyPair(SSHKeyManager.generateKeyPair());
                            } catch (JSchException e) {
                                throw new ParameterValidationException("Невозможно сгенерировать пару ключей:" + e.getMessage());
                            }
                        } else {
                            throw new ParameterValidationException("Для генерации новой пары ключей необходимо передать \"GENERATE\" в сообщении");
                        }
                        break;
                    case "crontab":
                        ObjectMapper mapper = new ObjectMapper();
                        List<CronTask> cronTasks = mapper.convertValue(entry.getValue(), new TypeReference<List<CronTask>>() {});
                        for (CronTask cronTask : cronTasks) {
                            if (cronTask != null) {
                                validateAndProcessCronTask(cronTask);
                            }
                        }
                        unixAccount.setCrontab(cronTasks);
                        break;
                    case "sendmailAllowed":
                        unixAccount.setSendmailAllowed((Boolean) serviceMessage.getParam("sendmailAllowed"));
                        if (!(Boolean) serviceMessage.getParam("sendmailAllowed")) {
                            personmgr.sendPhpMailNotificationToClient(unixAccount.getAccountId());
                        }
                        break;
                    case "switchedOn":
                        unixAccount.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "writable":
                        unixAccount.setWritable((Boolean) entry.getValue());
                        break;
                    case "quota":
                        try {
                            unixAccount.setQuota(getLongFromUnexpectedInput(entry.getValue()));
                        } catch (NumberFormatException e) {
                            throw new ParameterValidationException("Квота имеет неверный формат");
                        }
                        break;
                    case "solveQuarantine":
                        if (entry.getValue() != null) {
                            solveMalwareReport(resourceId);
                        }
                        break;
                    case "serverId":
                        unixAccount.setServerId((String) entry.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            log.error("Один из параметров указан неверно. " +
                    "UnixAccountId: " + unixAccount.getId() +
                    "AccountId: " + unixAccount.getAccountId() +
                    "ClassCastExceptionMessage: " + e.getMessage() +
                    "ServiceMessageParams: " + serviceMessage.getParams().toString()
            );
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(unixAccount);
        validate(unixAccount);
        store(unixAccount);

        return unixAccount;
    }

    public void updateQuotaUsed(String unixAccountId, Long quotaSize) {
        UnixAccount unixAccount = repository
                .findById(unixAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("UnixAccount с ID: " + unixAccountId + " не найден"));

        unixAccount.setQuotaUsed(quotaSize);

        store(unixAccount);
    }

    @Override
    public void preDelete(String resourceId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("unixAccountId", resourceId);

        if (governorOfFTPUser.buildAll(keyValue).size() > 0) {
            throw new ParameterValidationException("У UnixAccount'а есть FTPUser'ы");
        }

        if (governorOfWebSite.buildAll(keyValue).size() > 0) {
            throw new ParameterValidationException("У UnixAccount'а есть Website'ы");
        }
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ParameterValidationException("Не найден UnixAccount с ID: " + resourceId);
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    /**
     * Создает уникальное в рамках хостинга имя и домашнюю директорию
     * @param personalAccountId - id аккаунта в PM
     * @param name - желаемое имя пользователя. При совпадении добавит префикс _1 и т.д
     * @param homeDir - желаемая домашняя директория
     * @param excludeUnixAccountId - id - UnixAccount. Для этого id не будет проверяться совпадение homeDir и name
     * @return name и homeDir
     */
    private Pair<String, String> generateNameAndHomeDir(@Nullable String personalAccountId, @Nullable String name, @Nullable String homeDir, @Nullable String excludeUnixAccountId) {
        if (StringUtils.isEmpty(name)) {
            name = StringUtils.isNotEmpty(personalAccountId) && personalAccountId.matches("\\d+") ? "u" + personalAccountId : "u" + counterService.getNextUid();
        }
        if (StringUtils.isEmpty(homeDir)) {
            homeDir = "/home/" + name;
        }
        if (!exists("name", name, excludeUnixAccountId) && !exists("homeDir", homeDir, excludeUnixAccountId)) {
            return Pair.of(name, homeDir);
        }
        int prefix = 1;
        String newName = name;
        while (exists("name", newName, excludeUnixAccountId) || exists("homeDir", "/home/" + newName, excludeUnixAccountId)) {
            newName = name + "_" + prefix++;
        }
        return Pair.of(newName, "/home/" + newName);
    }

    @Override
    public UnixAccount buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        UnixAccount unixAccount = new UnixAccount();
        setResourceParams(unixAccount, serviceMessage, cleaner);

        if (unixAccount.getSwitchedOn() == null) {
            unixAccount.setSwitchedOn(true);
        }

        boolean replaceUidAndHome = Boolean.TRUE.equals(serviceMessage.getParam("replaceUidAndHome"));

        Integer uid = (Integer) serviceMessage.getParam("uid");
        if (uid == null || uid == 0) {
            uid = getFreeUid();
        } else if (exists("uid", uid, unixAccount.getId())) {
            if (replaceUidAndHome) {
                uid = getFreeUid();
            } else {
                throw new ParameterValidationException("uid: " + uid + " уже существует");
            }
        }

        String homeDir;
        String name;
        String serverId;
        Long quota;
        Long quotaUsed;
        String passwordHash;
        String password;
        List<CronTask> cronTasks = new ArrayList<>();
        String publicKey = null;
        String privateKey = null;
        Boolean sendmailAllowed = null;
        Boolean switchedOn = null;
        Boolean writable = null;

        try {
            homeDir = cleaner.cleanString((String) serviceMessage.getParam("homeDir"));
            name = cleaner.cleanString((String) serviceMessage.getParam("name"));
            if (!replaceUidAndHome && StringUtils.isNotEmpty(homeDir)) {
                throwIfExists("homeDir", homeDir, unixAccount.getAccountId());
            }
            if (!replaceUidAndHome && StringUtils.isNotEmpty(name)) {
                throwIfExists("name", name, unixAccount.getAccountId());
            }
            Pair<String, String> nameAndHome = generateNameAndHomeDir(unixAccount.getAccountId(),
                    name,
                    homeDir, unixAccount.getId()
            );
            name = nameAndHome.getFirst();
            homeDir = nameAndHome.getSecond();

            serverId = cleaner.cleanString((String) serviceMessage.getParam("serverId"));
            if (serverId == null || serverId.equals("")) {
                Object businessServices = serviceMessage.getParam("businessServices");
                serverId = getActiveHostingServerId(businessServices != null && (Boolean) businessServices);
            }

            if (serviceMessage.getParam("quota") == null) {
                throw new ParameterValidationException("Квота не может быть нуль");
            }

            quota = ((Number) serviceMessage.getParam("quota")).longValue();

            if (serviceMessage.getParam("quotaUsed") == null) {
                quotaUsed = 0L;
            } else {
                quotaUsed = ((Number) serviceMessage.getParam("quotaUsed")).longValue();
            }

            passwordHash = cleaner.cleanString((String) serviceMessage.getParam("passwordHash"));
            password = cleaner.cleanString((String) serviceMessage.getParam("password"));
            if (!(password == null || password.equals(""))) {
                try {
                    passwordHash = PasswordManager.forUbuntu(password);
                } catch (UnsupportedEncodingException e) {
                    throw new ParameterValidationException("Невозможно обработать пароль:" + password);
                }
            }
            if (serviceMessage.getParam("crontab") != null) {
                cronTasks = (List<CronTask>) serviceMessage.getParam("crontab");
            }
            if (serviceMessage.getParam("keyPair") instanceof Map) {
                Map<String, String> keyPair = (Map<String, String>) serviceMessage.getParam("keyPair");
                publicKey = cleaner.cleanString(keyPair.get("publicKey"));
                privateKey = StringUtils.trimToNull(cleaner.cleanString(keyPair.get("privateKey")));
            }
            if (serviceMessage.getParam("sendmailAllowed") != null) {
                sendmailAllowed = (Boolean) serviceMessage.getParam("sendmailAllowed");
            }
            if (serviceMessage.getParam("writable") != null) {
                writable = (Boolean) serviceMessage.getParam("writable");
            } else {
                writable = true;
            }
            if (serviceMessage.getParam("switchedOn") != null) {
                switchedOn = (Boolean) serviceMessage.getParam("switchedOn");
            }

        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }


        unixAccount.setUid(uid);
        unixAccount.setName(name);
        unixAccount.setHomeDir(homeDir);
        unixAccount.setServerId(serverId);
        unixAccount.setCrontab(cronTasks);
        unixAccount.setQuota(quota);
        unixAccount.setQuotaUsed(quotaUsed);
        unixAccount.setPasswordHash(passwordHash);
        unixAccount.setWritable(writable);
        unixAccount.setSendmailAllowed(sendmailAllowed);
        unixAccount.setSwitchedOn(switchedOn);
        try {
            if (StringUtils.isNotEmpty(publicKey)) {
                unixAccount.setKeyPair(new SSHKeyPair(privateKey, publicKey));
            } else {
                unixAccount.setKeyPair(SSHKeyManager.generateKeyPair());
            }
        } catch (JSchException e) {
            throw new ParameterValidationException("Невозможно сгенерировать пару ключей:" + e.getMessage());
        }

        return unixAccount;
    }

    @Override
    public void preValidate(UnixAccount unixAccount) {
        if (unixAccount.getSendmailAllowed() == null) {
            unixAccount.setSendmailAllowed(true);
        }
    }

    @Override
    public void validate(UnixAccount unixAccount) throws ParameterValidationException {
        Set<ConstraintViolation<UnixAccount>> constraintViolations = validator.validate(unixAccount, UnixAccountChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("unixAccount: " + unixAccount + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(UnixAccount unixAccount) {
        Set<ConstraintViolation<UnixAccount>> constraintViolations = validator.validate(unixAccount, UnixAccountChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] unixAccount: " + unixAccount + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    protected UnixAccount construct(UnixAccount unixAccount) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public UnixAccount build(String resourceId) throws ResourceNotFoundException {
        UnixAccount unixAccount = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Не найден UnixAccount с ID: " + resourceId));

        unixAccount.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(unixAccount.getId(), false));
        return unixAccount;
    }

    @Override
    public UnixAccount build(Map<String, String> keyValue) throws ResourceNotFoundException {
        UnixAccount unixAccount = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            unixAccount = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (unixAccount == null) {
            throw new ResourceNotFoundException("Не найден UnixAccount с ID: " + keyValue.get("resourceId"));
        }

        unixAccount.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(unixAccount.getId(), false));

        return unixAccount;
    }

    @Override
    public Collection<UnixAccount> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<UnixAccount> buildedUnixAccounts = new ArrayList<>();

        if (keyValue.get("accountId") != null) {
            buildedUnixAccounts = repository.findByAccountId(keyValue.get("accountId"));

            buildedUnixAccounts.forEach(item -> item.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(item.getId(), false)));
        } else if (keyValue.get("serverId") != null) {
            buildedUnixAccounts = repository.findByServerId(keyValue.get("serverId"));

            List<MalwareReport> malwared = malwareReportRepository.findBySolved(false);

            buildedUnixAccounts.forEach(item -> malwared
                    .stream()
                    .filter(m -> m.getUnixAccountId().equals(item.getId()))
                    .findFirst()
                    .ifPresent(k -> item.setInfected(true))
            );
        } else if (keyValue.get("name") != null) {
            buildedUnixAccounts = repository.findUnixAccountsByName(keyValue.get("name"));

            buildedUnixAccounts.forEach(item -> item.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(item.getId(), false)));
        }

        return buildedUnixAccounts;
    }

    public Collection<UnixAccount> buildAllPm(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<UnixAccount> unixAccounts = new ArrayList<>();

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);

        if (keyValue.get("serverId") != null) {
            log.info("[start] searchForUnixAccount");

            MongoCollection unixAccountsCollection = jongo.getCollection("unixAccounts");

            try (MongoCursor<UnixAccount> unixAccountCursor = unixAccountsCollection
                    .find("{serverId:#}", keyValue.get("serverId"))
                    .projection("{accountId: 1, serverId: 1}")
                    .map(this::setUnixAcFieldsForPm)
            ) {
                while (unixAccountCursor.hasNext()) {
                    unixAccounts.add(unixAccountCursor.next());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            log.info("[end] searchForUnixAccount");
        }

        return unixAccounts;
    }

    private UnixAccount setUnixAcFieldsForPm(DBObject result) {
        UnixAccount unixAccount = new UnixAccount();

        if (result.get("_id") instanceof ObjectId) {
            unixAccount.setId(((ObjectId) result.get("_id")).toString());
        } else if (result.get("_id") instanceof String) {
            unixAccount.setId((String) result.get("_id"));
        }

        unixAccount.setAccountId((String) result.get("accountId"));
        unixAccount.setServerId((String) result.get("serverId"));

        return unixAccount;
    }

    @Override
    public Collection<UnixAccount> buildAll() {
        List<UnixAccount> unixAccounts = repository.findAll();

        List<MalwareReport> malwared = malwareReportRepository.findBySolved(false);

        unixAccounts.forEach(item -> malwared
                .stream()
                .filter(m -> m.getUnixAccountId().equals(item.getId()))
                .findFirst()
                .ifPresent(k -> item.setInfected(true))
        );

        return unixAccounts;
    }

    @Override
    public void store(UnixAccount unixAccount) {
        repository.save(unixAccount);
    }

    public String getFreeUnixAccountName(String accountId) {
        if (accountId == null || !accountId.matches("^\\d+$")) {
            accountId = "";
        }

        if (accountId.isEmpty()) {
            accountId = counterService.getNextUid().toString();
        }

        if (exists("name", "u" + accountId)) {
            int suffix = 1;
            while (exists("name", "u" + accountId + "_" + suffix)) {
                suffix++;
            }
            return "u" + accountId + "_" + suffix;
        } else {
            return "u" + accountId;
        }
    }

    private String getActiveHostingServerId(boolean businessServices) {
        return staffRcClient.getActiveHostingServer(businessServices).getId();
    }

    public Integer getFreeUid() {
        Integer nextUid = counterService.getNextUid();
        while (exists("uid", nextUid)) {
            nextUid = counterService.getNextUid();
        }
        return nextUid;
    }

    public Integer getUnixAccountNameAsInteger(String unixAccountName) {
        if (!nameIsNumerable(unixAccountName)) {
            throw new ParameterValidationException("Имя " + unixAccountName + " не может быть приведено к числовому виду");
        }
        return Integer.parseInt(unixAccountName.replace("u", ""));
    }

    public Boolean nameIsNumerable(String name) {
        String pattern = "^u\\d+$";
        return name.matches(pattern);
    }

    private void validateAndProcessCronTask(CronTask cronTask) throws ParameterValidationException {
        if (cronTask.getExecTime() == null) {
            throw new ParameterValidationException("Не указано время исполнения");
        }
        if (cronTask.getCommand() == null) {
            throw new ParameterValidationException("Не указана команда для исполнения");
        }
        if (cronTask.getSwitchedOn() == null) {
            cronTask.setSwitchedOn(true);
        }

        try {
            cronTask.setExecTime(cronTask.getExecTime());
        } catch (IllegalArgumentException e) {
            throw new ParameterValidationException("Неверный формат времени выполнения задания");
        }
    }

    public void processMalwareReport(MalwareReport report) {
        List<MalwareReport> stored = malwareReportRepository.findByUnixAccountId(report.getUnixAccountId());

        if (stored.size() > 0) {
            stored.get(0).setInfectedFiles(report.getInfectedFiles());
            report = stored.get(0);
        }

        report.setSolved(false);

        if (!malwareReportRepository.existsByUnixAccountIdAndSolved(report.getUnixAccountId(), false)) {
            UnixAccount unixAccount = build(report.getUnixAccountId());
            if (unixAccount == null) throw new ResourceNotFoundException("При обработке отчёта не удалось найти UnixAccount");
            publisher.publishEvent(new UnixAccountInfectNotifyEvent(unixAccount.getAccountId()));
        }

        malwareReportRepository.save(report);
    }

    public MalwareReport getMalwareReport(String accountId, String unixAccountId) {
        if (repository.findByIdAndAccountId(unixAccountId, accountId) == null) {
            throw new ResourceNotFoundException("На аккаунте " + accountId + " не найден UnixAccount с ID: " + unixAccountId);
        }
        List<MalwareReport> reports = malwareReportRepository.findByUnixAccountId(unixAccountId);
        if (reports.size() == 0) return null;
        return reports.get(0);
    }

    private void solveMalwareReport(String unixAccountId) {
        List<MalwareReport> stored = malwareReportRepository.findByUnixAccountId(unixAccountId);
        MalwareReport report = new MalwareReport();

        if (stored.size() > 0) {
            report = stored.get(0);
        }

        report.setSolved(true);

        malwareReportRepository.save(report);
    }

    private void throwIfExists(String property, Object value) {
        if (exists(property, value)) {
            throw new ParameterValidationException(property + " " + value + " уже существует");
        }
    }

    private void throwIfExists(String property, Object value, @Nullable String excludeUnixAccountId) {
        if (exists(property, value, excludeUnixAccountId)) {
            throw new ParameterValidationException(property + " " + value + " уже существует");
        }
    }

    private boolean exists(String property, Object value) {
        return mongoOperations.exists(new Query(new Criteria(property).is(value)), UnixAccount.class);
    }

    private boolean exists(String property, Object value, @Nullable String excludeUnixAccountId) {
        if (StringUtils.isEmpty(excludeUnixAccountId)) {
            return exists(property, value);
        }
        return mongoOperations.exists(new Query(new Criteria(property).is(value).and("_id").ne(excludeUnixAccountId)), UnixAccount.class);
    }

    public void processQuotaReport(ServiceMessage serviceMessage) {
        Integer uid = null;
        String host = null;
        Long quotaUsed = null;

        if (serviceMessage.getParam("uid") != null) {
            uid = Integer.parseInt((String) serviceMessage.getParam("uid"));
        }

        if (serviceMessage.getParam("host") != null) {
            host = (String) serviceMessage.getParam("host");
        }

        if (serviceMessage.getParam("quotaUsed") != null) {
            Object quotaUsedFromMessage = serviceMessage.getParam("quotaUsed");
            if (quotaUsedFromMessage instanceof Long) {
                quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
            } else if (quotaUsedFromMessage instanceof Integer) {
                quotaUsed = ((Integer) serviceMessage.getParam("quotaUsed")).longValue();
            }
        }

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);

        MongoCollection unixAccountsCollection = jongo.getCollection("unixAccounts");

        if (uid != null && host != null && quotaUsed != null) {
            List<Server> servers = staffRcClient.getCachedServersOnlyIdAndNameByName(host);
            if (!servers.isEmpty()) {
                UnixAccount currentUnixAccount = unixAccountsCollection
                        .findOne("{uid: #, serverId: #}", uid, servers.get(0).getId())
                        .projection("{quotaUsed: 1}")
                        .map(
                                result -> {
                                    UnixAccount unixAccount = new UnixAccount();

                                    if (result.get("_id") instanceof ObjectId) {
                                        unixAccount.setId(((ObjectId) result.get("_id")).toString());
                                    } else if (result.get("_id") instanceof String) {
                                        unixAccount.setId((String) result.get("_id"));
                                    }

                                    unixAccount.setQuotaUsed((Long) result.get("quotaUsed"));
                                    return unixAccount;
                                }
                        );
                if (currentUnixAccount != null && !currentUnixAccount.getQuotaUsed().equals(quotaUsed)) {
                    log.info("unixAccounts quotaReport for host '" + host + "' and uid '" + uid + "' found changed quotaUsed. Old: " + currentUnixAccount.getQuotaUsed().toString() + " new: " + quotaUsed);

                    Object objectId = currentUnixAccount.getId();

                    try {
                        objectId = new ObjectId(currentUnixAccount.getId());
                    } catch (Exception ignored) {}

                    WriteResult writeResult = unixAccountsCollection.update("{_id: #}", objectId).with("{$set: {quotaUsed: #}}", quotaUsed);
                }
            }
        }
    }

    @Nonnull
    public byte[] getPuttyKey(String accountId, int unixAccountIndex) throws ResourceNotFoundException, InternalApiException {
        if (puttygenPath == null || puttygenPath.isEmpty()) {
            throw new InternalApiException("Создание ключей в формате PPK невозможно");
        }
        List<UnixAccount> unixAccounts = repository.findByAccountId(accountId);
        if (unixAccounts == null || unixAccounts.size() <= unixAccountIndex || unixAccounts.get(unixAccountIndex) == null) {
            throw new ResourceNotFoundException("Не найден Unix-аккаунт");
        }
        UnixAccount unixAccount = unixAccounts.get(unixAccountIndex);
        String privateKeyPem = unixAccount.getKeyPair().getPrivateKey();
        if (privateKeyPem == null  || privateKeyPem.isEmpty()) {
            throw new ResourceNotFoundException("На unix-аккаунте отсутствует приватный ключ");
        }
        try {
            byte[] pemBytes = SSHKeyManager.convertPemToPpk(privateKeyPem, puttygenPath);
            if (pemBytes == null || pemBytes.length == 0) {
                log.error("Got empty putty private key");
                throw new InternalApiException("Не удалось создать приватный ключ в формате PPK");
            }
            return pemBytes;
        } catch (IOException | InterruptedException e) {
            log.error("We got exception when generate putty private key", e);
            throw new InternalApiException("Не удалось создать приватный ключ в формате PPK");
        }
    }
}
