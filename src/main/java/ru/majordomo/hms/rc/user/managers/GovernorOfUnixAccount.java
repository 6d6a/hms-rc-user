package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfUnixAccount extends LordOfResources {

    public final int MIN_UID = 2000;
    public final int MAX_UID = Integer.MAX_VALUE;

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
        if (password != null || !password.equals("")) {
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
    public Resource build(String resourceId) throws ResourceNotFoundException {
        UnixAccount unixAccount = repository.findOne(resourceId);
        return unixAccount;
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
        Page<UnixAccount> page = repository.findAll(new PageRequest(0, 20));
        int freeNumName = 0;
        if (!page.hasContent()) {
            return "u" + MIN_UID;
        }
        pageLoop:
        do {
            int[] curPageAccIds = new int[20];
            int i = 0;
            List<UnixAccount> unixAccounts = page.getContent();
            for (UnixAccount unixAccount: unixAccounts) {
                String name = unixAccount.getName();
                if (nameIsNumerable(name)) {
                    curPageAccIds[i] = getUnixAccountNameAsInteger(name);
                    i++;
                }
            }
            Arrays.sort(curPageAccIds);
            for (int it = 0; (it < curPageAccIds.length - 1); it++) {
                if (curPageAccIds[it] == 0) {
                    continue;
                }
                int curNumName = curPageAccIds[it];
                int nextNumName = curPageAccIds[it+1];
                if ((nextNumName - curNumName) > 1) {
                    freeNumName = curNumName + 1;
                    break pageLoop;
                }
            }

        } while (page.hasNext());
        if (freeNumName == 0 || freeNumName < 0) {
            throw new IllegalStateException("Невозможно найти свободное имя");
        }

        return "u" + freeNumName;
    }

    private String getActiveHostingServerId() {
        return staffRcClient.getActiveHostingServers().getId();
    }

    /**
        Функция для получения свободного UID. Алгоритмы получения (в порядке использования):
        1) получаем наибольший UID и добавляем к нему 1;
        2) получаем наименьший UID и вычитаем из него 1;
        3) получаем отсортированный список UID'ов пачками по 20 и сравниваем их разницу.
        Алгоритм 3 на примере:
        - список UID'ов 2000,2002,2005,2006,2007;
        - берем первую пару, т.е. UID'ы 2000 и 2002;
        - вычитаем из наибольшего наименьшее, т.е. 2002-2000;
        - результат операции вычитания 2, он не равен 1, значит в последовательности найдена бреш;
        - получаем число, которое пропущено, для этого берем минимальное и прибавляем к нему 1;
        - 2000+1=2001 - свободный UID.
        Если ни один из алгоритмов не сработал, скорее всего свободные UID'ы закончились, выбрасываем
        IllegalStateException.
     */
    public Integer getFreeUid() {
        UnixAccount unixAccount;
        Integer freeUid;
        // алгоритм 1
        unixAccount = repository.findFirstByOrderByUidDesc();
        if (unixAccount == null) {
            freeUid = MIN_UID;
        } else {
            freeUid = unixAccount.getUid() + 1;
        }
        // алгоритм 2
        if (!isUidValid(freeUid)) {
            unixAccount = repository.findFirstByOrderByUidAsc();
            freeUid = unixAccount.getUid() - 1;
        }

        // алгоритм 3
        if (!isUidValid(freeUid)) {
            Page<UnixAccount> page = repository.findAllByOrderByUidAsc(new PageRequest(0, 20));
            pageLoop:
            do {
                List<UnixAccount> unixAccountList = page.getContent();
                for (int i = 0; i < (unixAccountList.size() - 1); i++) {
                    Integer curUnixAccountUid = unixAccountList.get(i).getUid();
                    Integer nextUnixAccountUid = unixAccountList.get((i + 1)).getUid();
                    if ((nextUnixAccountUid - curUnixAccountUid) != 1 &&
                            isUidValid(curUnixAccountUid + 1)) {
                        freeUid = curUnixAccountUid + 1;
                        break pageLoop;
                    }
                }
                if (page.hasNext()) {
                    page = repository.findAllByOrderByUidAsc(page.nextPageable());
                }
            } while (page.hasNext());
        }

        if (!isUidValid(freeUid)) {
            throw new IllegalStateException("MIN_UID: " + MIN_UID + "\nMAX_UID: " + MAX_UID + "\nfreeUID: " + freeUid + " некорректен\nНе удалось найти корректный и свободный UID.");
        }

        return freeUid;
    }

    public Integer getUnixAccountNameAsInteger(String unixAccountName) {
        if (!nameIsNumerable(unixAccountName)) {
            throw new ParameterValidateException("Имя " + unixAccountName + " не может быть приведено к числовому виду");
        }
        return Integer.parseInt(unixAccountName.replace("u",""));
    }

    public Boolean nameIsNumerable(String name) {
        String pattern = "^u\\d+$";
        return name.matches(pattern);
    }

    public Boolean isUidValid(Integer uid) {
        return (uid <= MAX_UID && uid >= MIN_UID);
    }

}
