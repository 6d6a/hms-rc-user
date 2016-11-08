package ru.majordomo.hms.rc.user.managers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.PasswordManager;
import ru.majordomo.hms.rc.user.common.SSHKeyManager;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfUnixAccount extends LordOfResources {

    public final int MIN_UID = 2000;
    public final int MAX_UID = 65535;

    private UnixAccountRepository repository;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    @Autowired
    public void setRepository(UnixAccountRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        UnixAccount unixAccount;

        try {

            unixAccount = (UnixAccount) buildResourceFromServiceMessage(serviceMessage);
            validate(unixAccount);
            store(unixAccount);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return unixAccount;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        UnixAccount unixAccount = new UnixAccount();
        LordOfResources.setResourceParams(unixAccount, serviceMessage, cleaner);

        if (unixAccount.getSwitchedOn() == null) {
            unixAccount.setSwitchedOn(true);
        }

        Integer uid = (Integer) serviceMessage.getParam("uid");
        if (uid == null || uid == 0) {
            uid = getFreeUid();
        }

        if (unixAccount.getName() == null || unixAccount.getName().equals("")) {
            unixAccount.setName(getFreeUnixAccountName());
        }

        String homeDir = cleaner.cleanString((String) serviceMessage.getParam("homeDir"));
        if (homeDir == null || homeDir.equals("")) {
            homeDir = "/home/" + unixAccount.getName();
        }

        String serverId = cleaner.cleanString((String) serviceMessage.getParam("serverId"));
        if (serverId == null || serverId.equals("")) {
            serverId = getActiveHostingServerId();
        }

        Long quota = (Long) serviceMessage.getParam("quota");
        if (quota == null || quota == 0) {
            throw new ParameterValidateException("Квота не может быть нуль");
        }

        Long quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
        if (quotaUsed == null) {
            quotaUsed = 0L;
        }

        String passwordHash = cleaner.cleanString((String) serviceMessage.getParam("passwordHash"));
        String password = cleaner.cleanString((String) serviceMessage.getParam("password"));
        if (!(password == null || password.equals(""))) {
            try {
                passwordHash = PasswordManager.forUbuntu(password);
            } catch (UnsupportedEncodingException e) {
                throw new ParameterValidateException("Невозможно обработать пароль:" + password);
            }
        }

        List<CronTask> cronTasks = (List<CronTask>) serviceMessage.getParam("cronTasks");
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
        try {
            unixAccount.setKeyPair(SSHKeyManager.generateKeyPair());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new ParameterValidateException("Невозможно сгенерировать пару ключей:" + e.getMessage());
        }

        return unixAccount;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        UnixAccount unixAccount = (UnixAccount) resource;
        if (unixAccount.getName() == null || unixAccount.getName().equals("")) {
            throw new ParameterValidateException("Имя unixAccount'а не может быть пустым");
        }
        if (unixAccount.getUid() == null || !isUidValid(unixAccount.getUid())) {
            throw new ParameterValidateException("UID unixAccount'а не может быть пустым");
        }
        if (unixAccount.getAccountId() == null || unixAccount.getAccountId().equals("")) {
            throw new ParameterValidateException("accountId не может быть пустым");
        }
        if (unixAccount.getHomeDir() == null || unixAccount.getHomeDir().equals("")) {
            throw new ParameterValidateException("homedir не может быть пустым");
        }

        if (unixAccount.getHomeDir().equals("/home") ||
                unixAccount.getHomeDir().equals("/home/") ||
                unixAccount.getHomeDir().equals("/")) {
            throw new ParameterValidateException("homedir не может быть /home или /");
        }
    }

    @Override
    protected Resource construct(Resource resource) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        return repository.findOne(resourceId);
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<UnixAccount> buildedUnixAccounts = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            buildedUnixAccounts = repository.findByAccountId(keyValue.get("accountId"));
        }

        return buildedUnixAccounts;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        UnixAccount unixAccount = (UnixAccount) resource;
        repository.save(unixAccount);
    }

    public String getFreeUnixAccountName() {
        List<UnixAccount> unixAccounts = repository.findAll();
        int counter = 0;
        int freeNumName = 0;
        int accountsCount = unixAccounts.size();


        if (accountsCount == 0) {
            freeNumName = MIN_UID;
        } else {

            int[] names = new int[unixAccounts.size()];
            for (UnixAccount unixAccount : unixAccounts) {
                String name = unixAccount.getName();
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
        List<UnixAccount> unixAccounts = repository.findAll();
        int freeUid = 0;
        int accountsCount = unixAccounts.size();

        if (accountsCount == 0) {
            freeUid = MIN_UID;
        } else {

            int[] uids = new int[accountsCount];
            int counter = 0;
            for (UnixAccount unixAccount : unixAccounts) {
                uids[counter] = unixAccount.getUid();
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
            throw new ParameterValidateException("Имя " + unixAccountName + " не может быть приведено к числовому виду");
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

}
