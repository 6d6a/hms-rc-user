package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import org.apache.commons.lang.NotImplementedException;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

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
import ru.majordomo.hms.rc.user.resources.DTO.ObjectContainer;
import ru.majordomo.hms.rc.user.resources.MalwareReport;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.validation.group.UnixAccountChecks;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static ru.majordomo.hms.rc.user.common.Utils.getLongFromUnexpectedInput;

@Component
public class GovernorOfUnixAccount extends LordOfResources<UnixAccount> {

    public final int MIN_UID = 2000;
    public final int MAX_UID = 65535;

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

    @Override
    public UnixAccount buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        UnixAccount unixAccount = new UnixAccount();
        setResourceParams(unixAccount, serviceMessage, cleaner);

        if (unixAccount.getSwitchedOn() == null) {
            unixAccount.setSwitchedOn(true);
        }

        Integer uid = (Integer) serviceMessage.getParam("uid");
        if (uid == null || uid == 0) {
            uid = getFreeUid();
        }

        unixAccount.setName(getFreeUnixAccountName());

        String homeDir;
        String serverId;
        Long quota;
        Long quotaUsed;
        String passwordHash;
        String password;
        List<CronTask> cronTasks = new ArrayList<>();

        try {
            homeDir = cleaner.cleanString((String) serviceMessage.getParam("homeDir"));
            if (homeDir == null || homeDir.equals("")) {
                homeDir = "/home/" + unixAccount.getName();
            }

            serverId = cleaner.cleanString((String) serviceMessage.getParam("serverId"));
            if (serverId == null || serverId.equals("")) {
                serverId = getActiveHostingServerId();
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
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }
        Boolean writable = (Boolean) serviceMessage.getParam("writable");
        if (writable == null) {
            writable = true;
        }

        unixAccount.setUid(uid);
        unixAccount.setHomeDir(homeDir);
        unixAccount.setServerId(serverId);
        unixAccount.setCrontab(cronTasks);
        unixAccount.setQuota(quota);
        unixAccount.setQuotaUsed(quotaUsed);
        unixAccount.setPasswordHash(passwordHash);
        unixAccount.setWritable(true);
        unixAccount.setSendmailAllowed(true);
        try {
            unixAccount.setKeyPair(SSHKeyManager.generateKeyPair());
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
        } else if (keyValue.get("serverId") != null) {
            buildedUnixAccounts = repository.findByServerId(keyValue.get("serverId"));
        }

        for (UnixAccount unixAccount : buildedUnixAccounts) {
            unixAccount.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(unixAccount.getId(), false));
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
                    .map(
                            result -> {
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
                    )
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

    @Override
    public Collection<UnixAccount> buildAll() {
        List<UnixAccount> unixAccounts = repository.findAll();

        for (UnixAccount unixAccount : unixAccounts) {
            unixAccount.setInfected(malwareReportRepository.existsByUnixAccountIdAndSolved(unixAccount.getId(), false));
        }

        return unixAccounts;
    }

    @Override
    public void store(UnixAccount unixAccount) {
        repository.save(unixAccount);
    }

    public String getFreeUnixAccountName() {
        List<String> unixAccountNames = this.getUnixAccountNames();
        int counter = 0;
        int freeNumName = 0;
        int accountsCount = unixAccountNames.size();


        if (accountsCount == 0) {
            freeNumName = MIN_UID;
        } else {

            int[] names = new int[unixAccountNames.size()];
            for (String name : unixAccountNames) {
                if (nameIsNumerable(name)) {
                    names[counter] = getUnixAccountNameAsInteger(name);
                    counter++;
                }
            }
            freeNumName = getGapInOrder(names);
        }
        if (freeNumName == 0) {
            throw new IllegalStateException("Невозможно найти свободное имя");
        }

        return "u" + freeNumName;
    }

    private String getActiveHostingServerId() {
        return staffRcClient.getActiveHostingServer().getId();
    }

    private int getGapInOrder(int[] order) {
        int numInGap = 0;
        int lastElementIndex = order.length - 1;
        Arrays.sort(order);
        if (order[0] > MIN_UID) {
            numInGap = MIN_UID;
        }
        if (order[lastElementIndex] < MAX_UID) {
            numInGap = MAX_UID;
        }

        for (int i = 0; i <= (lastElementIndex-1); i++) {
            int curElement = order[i];
            int nextElement = order[i + 1];
            if ((nextElement - curElement) > 1) {
                numInGap = curElement + 1;
                break;
            }
        }

        return numInGap;
    }

    /**
     * Функция для получения свободного UID. Алгоритмы получения (в порядке использования):
     * 1) получаем наибольший UID и добавляем к нему 1;
     * 2) получаем наименьший UID и вычитаем из него 1;
     * 3) получаем отсортированный список UID'ов пачками по 20 и сравниваем их разницу.
     * Алгоритм 3 на примере:
     * - список UID'ов 2000,2002,2005,2006,2007;
     * - берем первую пару, т.е. UID'ы 2000 и 2002;
     * - вычитаем из наибольшего наименьшее, т.е. 2002-2000;
     * - результат операции вычитания 2, он не равен 1, значит в последовательности найдена бреш;
     * - получаем число, которое пропущено, для этого берем минимальное и прибавляем к нему 1;
     * - 2000+1=2001 - свободный UID.
     * Если ни один из алгоритмов не сработал, скорее всего свободные UID'ы закончились, выбрасываем
     * IllegalStateException.
     */
    public Integer getFreeUid() {
        List<Integer> uidList = getUidList();
        int freeUid = 0;
        int accountsCount = uidList.size();

        if (accountsCount == 0) {
            freeUid = MIN_UID;
        } else {

            int[] uids = new int[accountsCount];
            int counter = 0;
            for (int uid : uidList) {
                uids[counter] = uid;
                counter++;
            }

            freeUid = getGapInOrder(uids);
        }

        if (freeUid == 0) {
            throw new IllegalStateException("Невозможно найти свободный UID");
        }

        return freeUid;
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

    public Boolean isUidValid(Integer uid) {
        return (uid <= MAX_UID && uid >= MIN_UID);
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
        if (repository.findByIdAndAccountId(unixAccountId, accountId) == null) throw new ResourceNotFoundException();
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

    public List<String> getUnixAccountNames() {
        return this.getListFieldValue(String.class, "name", "unixAccounts");
    }

    public List<Integer> getUidList() {
        return this.getListFieldValue(Integer.class, "uid", "unixAccounts");
    }

    public <T extends Object> List<T> getListFieldValue(Class<T> clazz, String field, String collection){
        GroupOperation group = group().addToSet(field).as("object");

        List<ObjectContainer> list = mongoOperations.aggregate(
                newAggregation(group), collection, ObjectContainer.class
        ).getMappedResults();

        if (list == null || list.isEmpty()) { return new ArrayList<>(); }

        return list.get(0).getObject();
    }
}
